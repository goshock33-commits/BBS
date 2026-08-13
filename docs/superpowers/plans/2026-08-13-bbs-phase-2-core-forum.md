# BBS Phase 2 Core Forum Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the normalized core forum and management baseline: DTO-based post/comment/collect/category APIs, explicit content states, ownership checks, atomic counters, and usable administrator moderation pages.

**Architecture:** Keep the Spring Boot monolith and Thymeleaf frontend. MySQL remains the only source of truth in this phase; controllers use request/response DTOs, services own transactions and authorization, and MyBatis-Plus mappers perform persistence. Existing page routes remain as compatibility bridges while `/api/v1` becomes the canonical write API.

**Tech Stack:** Java 17, Spring Boot 4, Spring MVC, Spring Security Session, MyBatis-Plus, MySQL, Flyway, Thymeleaf, Layui/jQuery, JUnit 5, MockMvc, Testcontainers MySQL

---

## Scope Boundary

Included:

- Posts: create, list, detail, edit, owner delete, status-aware visibility.
- Comments: create, reply, paginated list, owner/post-owner delete.
- Collections: idempotent collect/cancel and collection list.
- Categories: enabled/disabled state, name/order editing, enabled-category validation.
- Content states: `NORMAL`, `HIDDEN`, `DELETED`; hidden reason and moderation record.
- Admin basics: post/comment moderation, user disable/restore, category CRUD, summary counts.
- DTO/API migration and the minimum Thymeleaf/Layui integration needed to exercise these flows.

Explicitly defer to later phases:

- Redis cache, view de-duplication, rate limiting and hot ranking: Phase 3.
- Outbox, RocketMQ, notifications and daily trend consumers: Phase 4.
- Elasticsearch, IK analysis and search highlighting: Phase 5.
- RAG/AI assistant: Phase 6.
- Compose, fault injection, full E2E and performance targets: Phase 7.

## Task 1: Establish Phase 2 Baseline and Schema Migration

**Files:**
- Inspect: `src/main/resources/db/migration/V1__baseline_schema.sql`
- Create: `src/main/resources/db/migration/V2__core_forum_state_and_moderation.sql`
- Create/modify: `src/test/java/com/bbs/bbs/migration/FlywayCoreForumMigrationTest.java`

1. Inspect the V1 column names and existing entity mappings before writing SQL.
2. Write a failing migration test proving V2 adds the required state, version, category-enabled, uniqueness and moderation structures.
3. Run the focused migration test and confirm RED against the unmodified schema.
4. Add the smallest V2 migration that:
   - preserves existing data;
   - maps existing post/comment values to `NORMAL` or `DELETED`;
   - adds post/comment state and optimistic-lock version fields;
   - adds category enabled state;
   - enforces one active collection per `(user_id, post_id)`;
   - prevents negative counters through guarded updates or database constraints;
   - creates `moderation_record` with target, action, reason, operator and timestamp.
5. Run the migration test and confirm GREEN against an empty MySQL database.
6. Verify migration behavior against a fixture containing pre-existing posts, comments and collections.
7. Commit: `feat: add Phase 2 forum state schema`.

## Task 2: Define DTOs, Enums and Stable Error Contracts

**Files:**
- Create: `src/main/java/com/bbs/bbs/web/dto/post/*`
- Create: `src/main/java/com/bbs/bbs/web/dto/comment/*`
- Create: `src/main/java/com/bbs/bbs/web/dto/collect/*`
- Create: `src/main/java/com/bbs/bbs/web/dto/category/*`
- Create: `src/main/java/com/bbs/bbs/web/dto/admin/*`
- Create: `src/main/java/com/bbs/bbs/web/model/ContentStatus.java`
- Modify if needed: `src/main/java/com/bbs/bbs/web/error/ErrorCode.java`
- Test: `src/test/java/com/bbs/bbs/web/dto/CoreForumValidationTest.java`

1. Write failing validation tests for title length, body length, comment length, invalid category, blank moderation reason, invalid page size and unsupported state transitions.
2. Run the focused tests and confirm RED.
3. Add immutable request records and response records; never bind `BBSPost`, `BBSPostComment`, `BBSPostCollect` or `BBSPostCategory` directly from JSON.
4. Include only server-derived identity in commands; no request DTO may accept the acting user ID.
5. Define stable errors for resource missing, forbidden ownership, invalid state transition, duplicate collection, disabled category and moderation reason required.
6. Run focused DTO tests and confirm GREEN.
7. Commit: `feat: define core forum DTO contracts`.

## Task 3: Normalize Post and Category Services

**Files:**
- Modify: `src/main/java/com/bbs/bbs/bean/BBSPost.java`
- Modify: `src/main/java/com/bbs/bbs/bean/BBSPostCategory.java`
- Modify: `src/main/java/com/bbs/bbs/Mapper/BBSPostMapper.java`
- Modify: `src/main/java/com/bbs/bbs/Mapper/BBSCategoryMapper.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostService.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostServiceImpl.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostCategoryService.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostCategoryServiceImp.java`
- Test: `src/test/java/com/bbs/bbs/service/CoreForumPostServiceTest.java`

1. Write failing service tests for enabled-category-only publishing, owner-only edit/delete, hidden/deleted visibility, author/admin visibility, and optimistic-lock conflict.
2. Run focused tests and confirm RED.
3. Implement service methods with constructor injection and explicit transaction boundaries:
   - public list/detail returns only `NORMAL` content;
   - author may view own `HIDDEN` content but cannot interact with it;
   - `DELETED` is never restorable by ordinary users;
   - create defaults to `NORMAL`, zero counters and version 0/1;
   - edit checks ownership and updates version;
   - delete is logical and idempotent;
   - category writes enforce unique names and enabled-state rules.
4. Replace whole-entity counter writes with guarded atomic mapper updates where applicable.
5. Run focused tests and then existing homepage/post tests.
6. Commit: `feat: normalize post and category services`.

## Task 4: Normalize Comment and Collection Services

**Files:**
- Modify: `src/main/java/com/bbs/bbs/bean/BBSPostComment.java`
- Modify: `src/main/java/com/bbs/bbs/bean/BBSPostCollect.java`
- Modify: `src/main/java/com/bbs/bbs/Mapper/BBSCommentMapper.java`
- Modify: `src/main/java/com/bbs/bbs/Mapper/BBSCollectMapper.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSCommentService.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSCommentServiceImpl.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostCollectService.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostCollectServiceImpl.java`
- Test: `src/test/java/com/bbs/bbs/service/CoreForumSocialServiceTest.java`

1. Write failing tests for reply-parent validation, comment ownership, post-owner deletion, hidden/deleted rejection, duplicate collect idempotency, cancel idempotency and non-negative counters.
2. Run focused tests and confirm RED.
3. Implement DTO-to-entity mapping inside services, not controllers.
4. Make comment/collect operations transactional and use guarded SQL counter updates; do not write Redis in Phase 2.
5. Enforce one active collection through the database unique key and translate duplicate-key races into a stable business result.
6. Return paginated DTO data without exposing password hashes or internal entity fields.
7. Run focused tests and existing controller/service regressions.
8. Commit: `feat: enforce forum social ownership and counters`.

## Task 5: Add Canonical `/api/v1` Forum APIs

**Files:**
- Create: `src/main/java/com/bbs/bbs/handller/api/PostApiController.java`
- Create: `src/main/java/com/bbs/bbs/handller/api/CommentApiController.java`
- Create: `src/main/java/com/bbs/bbs/handller/api/CollectApiController.java`
- Create: `src/main/java/com/bbs/bbs/handller/api/CategoryApiController.java`
- Test: `src/test/java/com/bbs/bbs/handller/api/CoreForumControllerTest.java`

1. Write failing MockMvc tests for anonymous read access, anonymous write 401, CSRF rejection, owner/non-owner authorization, DTO validation and stable `ApiResult/PageResult` envelopes.
2. Run focused controller tests and confirm RED.
3. Implement routes:
   - `GET /api/v1/posts` and `GET /api/v1/posts/{id}`;
   - `POST /api/v1/posts`, `PUT /api/v1/posts/{id}`, `DELETE /api/v1/posts/{id}`;
   - `GET /api/v1/posts/{id}/comments`, `POST /api/v1/posts/{id}/comments`, `DELETE /api/v1/comments/{commentId}`;
   - `PUT /api/v1/posts/{id}/collect` and `DELETE /api/v1/posts/{id}/collect`;
   - `GET /api/v1/categories` for enabled public categories;
   - authenticated/admin checks based on `@AuthenticationPrincipal`, never request user IDs.
4. Keep legacy page routes as compatibility bridges, delegating to the new services instead of duplicating business rules.
5. Run focused controller tests and all non-Docker tests.
6. Commit: `feat: expose normalized forum APIs`.

## Task 6: Implement Administrator Moderation APIs

**Files:**
- Create: `src/main/java/com/bbs/bbs/service/AdminModerationService.java`
- Create: `src/main/java/com/bbs/bbs/service/AdminModerationServiceImpl.java`
- Create: `src/main/java/com/bbs/bbs/handller/api/AdminApiController.java`
- Create/modify: admin DTOs from Task 2
- Test: `src/test/java/com/bbs/bbs/handller/api/AdminApiControllerTest.java`

1. Write failing tests proving USER receives 403 and ADMIN can filter and moderate.
2. Run focused tests and confirm RED.
3. Implement admin-only routes:
   - `GET /api/v1/admin/posts` with status/category/author/time filters;
   - `POST /api/v1/admin/posts/{id}/hide`, `/restore`, `/delete`;
   - `GET /api/v1/admin/comments` and equivalent hide/restore/delete actions;
   - `POST /api/v1/admin/users/{id}/disable` and `/restore`;
   - category create/update/enable/disable routes;
   - read-only total counts for users, posts and comments.
4. Require a nonblank reason for hide/delete actions and persist `moderation_record` with operator identity.
5. Enforce legal transitions (`NORMAL <-> HIDDEN -> DELETED`) and ensure admin cannot restore `DELETED` content.
6. Run focused admin tests and the complete non-Docker regression.
7. Commit: `feat: add administrator moderation APIs`.

## Task 7: Integrate Existing Thymeleaf/Layui Pages

**Files:**
- Modify: `src/main/resources/templates/index.html`
- Modify: `src/main/resources/templates/jie/add.html`
- Modify: `src/main/resources/templates/jie/detail.html`
- Modify: `src/main/resources/templates/jie/edit.html`
- Modify: `src/main/resources/templates/user/index.html`
- Modify: `src/main/resources/templates/header.html`
- Create: `src/main/resources/templates/admin/index.html`
- Create: `src/main/resources/templates/admin/posts.html`
- Create: `src/main/resources/templates/admin/comments.html`
- Create: `src/main/resources/templates/admin/users.html`
- Create: `src/main/resources/templates/admin/categories.html`
- Modify: page controllers as required for `/admin/**`
- Test: `src/test/java/com/bbs/bbs/handller/AdminPageControllerTest.java`

1. Write failing page/controller tests for anonymous admin redirect, USER 403 and ADMIN page 200.
2. Run focused page tests and confirm RED.
3. Add minimal usable admin pages for moderation, user state and categories; use the existing Layui/jQuery style.
4. Migrate post/comment/collect AJAX writes to `/api/v1` with the existing CSRF metadata/header setup.
5. Preserve public browsing and old URLs while removing direct entity JSON binding from new writes.
6. Run page tests and manually verify USER and ADMIN flows.
7. Commit: `feat: add core forum and admin pages`.

## Task 8: Phase 2 Verification and Handoff

**Files:**
- Modify: `HELP.md`
- Modify: `docs/superpowers/progress/2026-08-11-bbs-phase-1-progress.md` or create a Phase 2 progress record
- Create if needed: `.env.example` additions for Phase 2 only

1. Run Windows non-Docker tests, compilation and `git diff --check`.
2. Run Linux/Docker-owned commands only through the user: `mvn verify`, Flyway migration against empty MySQL, and API/page smoke checks.
3. Verify the acceptance matrix:
   - public NORMAL posts/comments visible;
   - HIDDEN invisible to guests and non-authors;
   - DELETED invisible and not restorable by ordinary users;
   - owner-only edit/delete works;
   - duplicate collect does not increase counts twice;
   - comment/reply counters never become negative;
   - disabled categories reject new posts;
   - USER admin routes return 403;
   - ADMIN moderation records reason/operator/time;
   - legacy pages still render.
4. Update the progress record with exact test counts and Linux evidence.
5. Commit: `docs: record Phase 2 verification`.
6. Do not merge to `main`; use the branch-finishing workflow only after the user explicitly chooses an integration option.

## Phase 2 Exit Criteria

- V2 migration succeeds on an empty database and preserves a fixture database.
- Every new write path uses DTO validation, CSRF, authentication and resource ownership/role checks.
- Public APIs never expose hidden/deleted content accidentally.
- Collection uniqueness and all aggregate counters are correct under repeated operations.
- Admin moderation is usable from a browser and records an auditable reason.
- Windows tests and compilation pass; Linux/Docker verification is recorded by the user.
- Redis, RocketMQ, ES, RAG and Compose are still untouched and remain Phase 3-7 work.
