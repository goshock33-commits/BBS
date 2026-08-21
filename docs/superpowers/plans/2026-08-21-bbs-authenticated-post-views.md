# Authenticated Post Views Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Require login for the legacy HTML post detail page and count each authenticated account at most once per post without Redis.

**Architecture:** Spring Security protects only `/detail/**` and continues to leave the normalized GET post API public. A V3 Flyway table stores unique `(post_id, user_id)` visits; a dedicated mapper performs duplicate-safe insertion, and the existing post service increments the MySQL counter only after the first successful insertion inside one transaction.

**Tech Stack:** Java 21, Spring Boot 4, Spring Security, MyBatis/MyBatis-Plus, Flyway, MySQL 8.4, JUnit 5, Mockito, MockMvc

---

## File Structure

- Create `src/main/resources/db/migration/V3__authenticated_post_views.sql`: persistent unique view records.
- Create `src/main/java/com/bbs/bbs/Mapper/BBSPostViewMapper.java`: duplicate-safe view insertion boundary.
- Modify `src/main/java/com/bbs/bbs/config/SecurityConfig.java`: protect only the HTML detail route.
- Modify `src/main/java/com/bbs/bbs/service/BBSPostServiceImpl.java`: transactional first-view registration and atomic counter update.
- Modify `src/test/java/com/bbs/bbs/config/SecurityConfigTest.java`: anonymous redirect and public API regression coverage.
- Create `src/test/java/com/bbs/bbs/migration/PostViewMigrationScriptTest.java`: V3 SQL contract coverage without Docker.
- Modify `src/test/java/com/bbs/bbs/migration/FlywayCoreForumMigrationTest.java`: V1-V3 MySQL migration and data-preservation assertions.
- Modify `src/test/java/com/bbs/bbs/service/BBSPostServiceImplTest.java`: first, duplicate, different-user, and hidden-post view behavior.

### Task 1: Protect Only the HTML Detail Route

**Files:**
- Modify: `src/test/java/com/bbs/bbs/config/SecurityConfigTest.java`
- Modify: `src/main/java/com/bbs/bbs/config/SecurityConfig.java`

- [ ] **Step 1: Write failing security tests**

Add test endpoints for `/detail/{postId}` and `GET /api/v1/posts/{postId}`. Add assertions that an anonymous page request redirects to `/login`, an authenticated page request returns 200, and the anonymous JSON GET remains 200.

```java
@Test
void postDetailRedirectsAnonymousUserToLogin() throws Exception {
    mvc.perform(get("/detail/7"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
}

@Test
void publicPostApiRemainsAvailableWithoutLogin() throws Exception {
    mvc.perform(get("/api/v1/posts/7"))
            .andExpect(status().isOk());
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
mvn.cmd -Dtest=SecurityConfigTest test
```

Expected: `postDetailRedirectsAnonymousUserToLogin` fails because `/detail/**` is currently `permitAll`.

- [ ] **Step 3: Implement the security rule**

Remove `"/detail/**"` from the public matcher and add it to the authenticated page matcher. Do not change the public GET API matchers.

```java
.requestMatchers("/detail/**", "/me/**", "/api/v1/**", ...)
.authenticated()
```

- [ ] **Step 4: Verify GREEN and commit**

Run `mvn.cmd -Dtest=SecurityConfigTest test`, expect all tests to pass, then commit only these two files with:

```text
fix: require login for post detail pages
```

### Task 2: Add the Persistent Unique View Schema

**Files:**
- Create: `src/test/java/com/bbs/bbs/migration/PostViewMigrationScriptTest.java`
- Create: `src/main/resources/db/migration/V3__authenticated_post_views.sql`
- Modify: `src/test/java/com/bbs/bbs/migration/FlywayCoreForumMigrationTest.java`

- [ ] **Step 1: Write a failing V3 script contract test**

Load `/db/migration/V3__authenticated_post_views.sql` and assert that it defines `tb_bbs_post_view`, `post_id`, `user_id`, and a unique key over `(post_id, user_id)`.

```java
assertTrue(sql.contains("create table tb_bbs_post_view"));
assertTrue(sql.contains("unique key uk_post_view_post_user (post_id, user_id)"));
```

- [ ] **Step 2: Verify RED**

Run `mvn.cmd -Dtest=PostViewMigrationScriptTest test` and expect failure because the V3 resource does not exist.

- [ ] **Step 3: Add the V3 migration**

Create the table without changing V1 or V2:

```sql
CREATE TABLE tb_bbs_post_view (
    view_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (view_id),
    UNIQUE KEY uk_post_view_post_user (post_id, user_id),
    KEY idx_post_view_user (user_id),
    CONSTRAINT fk_post_view_post FOREIGN KEY (post_id)
        REFERENCES tb_bbs_post (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_view_user FOREIGN KEY (user_id)
        REFERENCES tb_bbs_user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 4: Extend the MySQL migration test**

After V1-V3 migration, assert that `flyway_schema_history` reports version `3`, the new unique index exists, existing post/user rows remain present, the first `(post_id, user_id)` insert succeeds, and a duplicate insert is rejected.

- [ ] **Step 5: Verify available tests and commit**

Run `mvn.cmd -Dtest=PostViewMigrationScriptTest test`, expect PASS. Do not run Docker locally; the user will run the MySQL integration test on Linux later. Commit the migration and tests with:

```text
feat: persist unique authenticated post views
```

### Task 3: Count Only the First Authenticated View

**Files:**
- Create: `src/main/java/com/bbs/bbs/Mapper/BBSPostViewMapper.java`
- Modify: `src/test/java/com/bbs/bbs/service/BBSPostServiceImplTest.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostServiceImpl.java`

- [ ] **Step 1: Write failing service tests**

Mock a wished-for `BBSPostViewMapper.insertView(postId, userId)` method. Verify these behaviors separately:

```java
when(postViewMapper.insertView(7L, 42L)).thenReturn(1);
service.getBBSPostForDetail(7L, 42L);
verify(postMapper).incrementViewCount(7L);

when(postViewMapper.insertView(7L, 42L))
        .thenThrow(new DuplicateKeyException("already viewed"));
service.getBBSPostForDetail(7L, 42L);
verify(postMapper, never()).incrementViewCount(7L);
```

Also verify that two different user IDs can each increment once and that an author's `HIDDEN` detail does not call either mapper method.

- [ ] **Step 2: Verify RED**

Run `mvn.cmd -Dtest=BBSPostServiceImplTest test`. Expected: test compilation fails because `BBSPostViewMapper` and its service dependency do not exist yet.

- [ ] **Step 3: Add the duplicate-safe mapper**

```java
public interface BBSPostViewMapper {
    @Insert("""
            INSERT INTO tb_bbs_post_view (post_id, user_id, create_time)
            VALUES (#{postId}, #{userId}, CURRENT_TIMESTAMP(3))
            """)
    int insertView(@Param("postId") Long postId,
                   @Param("userId") Long userId);
}
```

- [ ] **Step 4: Implement transactional view registration**

Inject `BBSPostViewMapper`. Mark `getBBSPostForDetail` transactional. For `NORMAL` posts, call `insertView`; only when it returns `1` call `incrementViewCount` and update the returned in-memory counter. Catch `DuplicateKeyException` as an idempotent repeat visit; unrelated database failures still propagate. If counter update returns `0` after a new record, throw a runtime business exception so the transaction rolls back. Leave hidden/deleted visibility rules unchanged and do not call Redis.

- [ ] **Step 5: Verify GREEN and regression**

Run:

```powershell
mvn.cmd -Dtest=BBSPostServiceImplTest test
mvn.cmd "-Dtest=!FlywayMigrationTest,!FlywayCoreForumMigrationTest,!BbsApplicationTests" test
mvn.cmd -DskipTests compile
git diff --check
```

Expected: focused tests and all Windows non-Docker tests pass, compilation succeeds, and whitespace validation reports no errors.

- [ ] **Step 6: Commit**

Commit the mapper, service, and service test with:

```text
fix: count each authenticated post viewer once
```

### Task 4: Linux and Browser Verification

**Files:**
- Modify: `docs/superpowers/progress/2026-08-14-bbs-phase-2-progress.md`

- [ ] **Step 1: Push from the authenticated Windows terminal**

```powershell
git push origin phase1/baseline-security
```

- [ ] **Step 2: Ask the user to run Linux verification**

```bash
cd /root/BBS1
git pull --ff-only origin phase1/baseline-security
mvn verify
```

This verifies V1-V3 against MySQL 8.4, including the unique view constraint.

- [ ] **Step 3: Ask the user to verify browser behavior**

- Anonymous click on a post redirects to `/login`.
- After login, the first visit increases the view count by one.
- Refreshing or reopening the same post with the same account does not increase it again.
- Visiting with a different account increases it once.
- `GET /api/v1/posts/{id}` remains accessible without login.

- [ ] **Step 4: Record evidence and commit progress**

Update the Phase 2 progress record with exact Windows, Linux, migration, and browser results. Run `git diff --check` and commit with:

```text
docs: record authenticated post view verification
```
