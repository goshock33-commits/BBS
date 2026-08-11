# BBS Phase 1 Baseline and Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore a reproducible, compiling BBS baseline with Flyway-managed schema, validated request DTOs, unified API errors, Spring Security session authorization, and BCrypt password storage.

**Architecture:** Keep the existing Spring Boot/Thymeleaf monolith and MyBatis-Plus mappers. Introduce a small `web` boundary for request/response types and errors, then replace the hand-written login interceptor with Spring Security while retaining the existing HTML pages. This plan deliberately stops before Redis consistency, RocketMQ, Elasticsearch, notifications, and RAG.

**Tech Stack:** Java 17, Spring Boot 4.x, Spring MVC, Spring Security, Bean Validation, MyBatis-Plus, MySQL, Flyway, Thymeleaf, JUnit 5, Mockito, MockMvc, Testcontainers MySQL

---

## Scope and prerequisites

- Design source: `docs/superpowers/specs/2026-08-11-bbs-requirements-design.md`.
- This workspace currently has no `.git` directory. Commit steps below become mandatory after the workspace is initialized as a repository; until then, stop at each commit boundary and record the changed-file list.
- Use the locally installed Maven command `mvn.cmd`; the current `mvnw.cmd` fails before Maven starts in this Windows environment.
- Do not add Redis cache redesign, Outbox, RocketMQ, ES, notifications, admin pages, or RAG in this phase.
- End-state acceptance: `mvn.cmd verify` passes against the containerized test database, the application starts from an empty MySQL schema, anonymous users can browse, and authenticated/admin routes are enforced by Spring Security.

## File map

### Build and configuration

- Modify `pom.xml`: validation, security, Flyway, Actuator, Testcontainers, compiler/test configuration.
- Modify `src/main/resources/application.properties`: remove credentials and keep shared defaults.
- Create `src/main/resources/application-local.properties`: environment-variable-backed local connections.
- Create `src/test/resources/application-test.properties`: isolated test profile.
- Create `src/main/resources/db/migration/V1__baseline_schema.sql`: reproducible baseline schema and initial categories.
- Create `src/test/java/com/bbs/bbs/support/MySqlContainerSupport.java`: reusable dynamic MySQL properties for integration tests.

### Baseline compile repair

- Modify `src/main/java/com/bbs/bbs/service/BBSPostServiceImpl.java`: implement `getBBSPostPageForIndex` and return empty pages instead of `null`.
- Test `src/test/java/com/bbs/bbs/service/BBSPostServiceImplTest.java`: page filters, sorting, author enrichment, empty page.

### Web contracts and errors

- Create `src/main/java/com/bbs/bbs/web/response/ApiResult.java`: stable `code/message/traceId/data` envelope.
- Create `src/main/java/com/bbs/bbs/web/response/PageResult.java`: generic page envelope for new APIs.
- Create `src/main/java/com/bbs/bbs/web/error/ErrorCode.java`: stable business codes.
- Create `src/main/java/com/bbs/bbs/web/error/BusinessException.java`: expected business failure.
- Create `src/main/java/com/bbs/bbs/web/error/GlobalExceptionHandler.java`: validation, authorization, business and unexpected errors.
- Create `src/main/java/com/bbs/bbs/web/filter/TraceIdFilter.java`: request trace ID propagation.
- Create `src/main/java/com/bbs/bbs/web/filter/TraceIds.java`: read the current trace ID from MDC.
- Test `src/test/java/com/bbs/bbs/web/error/GlobalExceptionHandlerTest.java`.

### Authentication and DTOs

- Create `src/main/java/com/bbs/bbs/web/dto/auth/RegisterRequest.java`.
- Create `src/main/java/com/bbs/bbs/web/dto/auth/LoginRequest.java`.
- Create `src/main/java/com/bbs/bbs/web/dto/user/UpdateProfileRequest.java`.
- Create `src/main/java/com/bbs/bbs/web/dto/user/ChangePasswordRequest.java`.
- Modify `src/main/java/com/bbs/bbs/bean/BBSUser.java`: password hash, role, account status mapping.
- Modify `src/main/java/com/bbs/bbs/service/BBSUserService.java`: DTO-oriented account operations without `HttpSession` parameters.
- Modify `src/main/java/com/bbs/bbs/service/BBSUserServiceImp.java`: BCrypt registration/password change and account-state checks.
- Create `src/main/java/com/bbs/bbs/security/BbsUserDetailsService.java`.
- Create `src/main/java/com/bbs/bbs/config/SecurityConfig.java`.
- Delete `src/main/java/com/bbs/bbs/Interceptor/BBSLoginInterceptor.java` after security tests pass.
- Modify `src/main/java/com/bbs/bbs/config/MyBBSWebMvcConfigurer.java`: remove interceptor registration; retain upload resource mapping only.

### Controllers and templates

- Modify `src/main/java/com/bbs/bbs/handller/loginController.java`.
- Modify `src/main/java/com/bbs/bbs/handller/registerController.java`.
- Modify `src/main/java/com/bbs/bbs/handller/BBSUserController.java`.
- Modify `src/main/resources/templates/user/login.html`.
- Modify `src/main/resources/templates/user/reg.html`.
- Modify `src/main/resources/templates/user/set.html`.
- Modify `src/main/resources/templates/header.html`.
- Test `src/test/java/com/bbs/bbs/handller/AuthenticationControllerTest.java`.
- Test `src/test/java/com/bbs/bbs/handller/UserProfileControllerTest.java`.

## Task 1: Establish the failing baseline and dependency set

**Files:**
- Modify: `pom.xml`
- Create: `src/test/java/com/bbs/bbs/support/MySqlContainerSupport.java`
- Test: `src/test/java/com/bbs/bbs/BbsApplicationTests.java`

- [ ] **Step 1: Run the current compiler and capture the real first failure**

Run:

```powershell
mvn.cmd -DskipTests compile
```

Expected after dependencies resolve: FAIL because `BBSPostServiceImpl` does not implement `getBBSPostPageForIndex(PageQueryUtil)`.

- [ ] **Step 2: Add phase-1 dependencies**

Add these dependencies inside `<dependencies>` in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Make the context test use the shared test database**

Create `MySqlContainerSupport` before replacing the context test:

```java
package com.bbs.bbs.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

public abstract class MySqlContainerSupport {
    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("bbs")
                    .withUsername("bbs")
                    .withPassword("bbs");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
```

Then replace the existing empty context test:

```java
package com.bbs.bbs;

import com.bbs.bbs.support.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class BbsApplicationTests extends MySqlContainerSupport {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Re-run compile to confirm the missing service method remains the only source failure**

Run:

```powershell
mvn.cmd -DskipTests compile
```

Expected: FAIL naming `BBSPostServiceImpl` and `getBBSPostPageForIndex` rather than dependency resolution errors.

- [ ] **Step 5: Commit the build baseline if Git is initialized**

```powershell
git add pom.xml src/test/java/com/bbs/bbs/BbsApplicationTests.java src/test/java/com/bbs/bbs/support/MySqlContainerSupport.java
git commit -m "build: add baseline validation and security dependencies"
```

## Task 2: Repair homepage pagination with tests

**Files:**
- Modify: `src/main/java/com/bbs/bbs/service/BBSPostServiceImpl.java`
- Test: `src/test/java/com/bbs/bbs/service/BBSPostServiceImplTest.java`

- [ ] **Step 1: Write a failing empty-page test**

Create the test with Mockito-injected mappers and verify an empty `PageResult`, never `null`:

```java
package com.bbs.bbs.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.util.PageQueryUtil;
import com.bbs.util.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BBSPostServiceImplTest {
    @Mock BBSPostMapper postMapper;
    @Mock BBSUserMapper userMapper;
    @InjectMocks BBSPostServiceImpl service;

    @Test
    void getIndexPageReturnsEmptyPageInsteadOfNull() {
        Page<BBSPost> empty = new Page<>(1, 10);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(empty);

        PageResult result = service.getBBSPostPageForIndex(
                new PageQueryUtil(Map.of("page", 1, "limit", 10)));

        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getList().isEmpty());
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

```powershell
mvn.cmd -Dtest=BBSPostServiceImplTest test
```

Expected: compilation FAIL because `getBBSPostPageForIndex` is absent.

- [ ] **Step 3: Implement the missing method**

Add this method to `BBSPostServiceImpl`; keep legacy SQL keyword matching only until the ES phase replaces it:

```java
@Override
public PageResult getBBSPostPageForIndex(PageQueryUtil pageUtil) {
    int pageNumber = pageUtil.getPage();
    int pageSize = Math.min(pageUtil.getLimit(), 50);
    LambdaQueryWrapper<BBSPost> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(BBSPost::getPostStatus, 1)
            .eq(BBSPost::getIsDeleted, 0);

    Object categoryId = pageUtil.get("categoryId");
    if (categoryId != null) {
        wrapper.eq(BBSPost::getPostCategoryId,
                Long.parseLong(categoryId.toString()));
    }

    Object keyword = pageUtil.get("keyword");
    if (keyword != null && !keyword.toString().isBlank()) {
        String value = keyword.toString().trim();
        wrapper.and(w -> w.like(BBSPost::getPostTitle, value)
                .or().like(BBSPost::getPostContent, value));
    }

    Object period = pageUtil.get("period");
    if ("week".equals(period)) {
        wrapper.ge(BBSPost::getCreateTime,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(7)));
    } else if ("month".equals(period)) {
        wrapper.ge(BBSPost::getCreateTime,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(30)));
    }

    if ("views".equals(pageUtil.get("orderBy"))) {
        wrapper.orderByDesc(BBSPost::getPostViews);
    } else {
        wrapper.orderByDesc(BBSPost::getCreateTime);
    }

    IPage<BBSPost> page = bbsPostMapper.selectPage(
            new Page<>(pageNumber, pageSize), wrapper);
    List<BBSPost> records = page.getRecords();
    if (!records.isEmpty()) {
        List<Long> userIds = records.stream()
                .map(BBSPost::getPublishUserId)
                .distinct()
                .toList();
        Map<Long, BBSUser> users = bbsUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(BBSUser::getUserId, user -> user));
        records.forEach(post -> {
            BBSUser user = users.get(post.getPublishUserId());
            if (user != null) {
                post.setNickName(user.getNickName());
                post.setHeadImgUrl(user.getHeadImgUrl());
                post.setUser(user);
            }
        });
    }
    return new PageResult(records, Math.toIntExact(page.getTotal()),
            pageSize, pageNumber);
}
```

- [ ] **Step 4: Run service tests and compiler**

```powershell
mvn.cmd -Dtest=BBSPostServiceImplTest test
mvn.cmd -DskipTests compile
```

Expected: focused test PASS and compile `BUILD SUCCESS`.

- [ ] **Step 5: Commit the compile repair if Git is initialized**

```powershell
git add src/main/java/com/bbs/bbs/service/BBSPostServiceImpl.java src/test/java/com/bbs/bbs/service/BBSPostServiceImplTest.java
git commit -m "fix: restore homepage post pagination"
```

## Task 3: Add environment-backed configuration and Flyway baseline

**Files:**
- Modify: `src/main/resources/application.properties`
- Create: `src/main/resources/application-local.properties`
- Create: `src/test/resources/application-test.properties`
- Create: `src/main/resources/db/migration/V1__baseline_schema.sql`
- Test: `src/test/java/com/bbs/bbs/migration/FlywayMigrationTest.java`

- [ ] **Step 1: Write a migration test using the shared container support**

```java
package com.bbs.bbs.migration;

import com.bbs.bbs.support.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
class FlywayMigrationTest extends MySqlContainerSupport {
    @Autowired DataSource dataSource;

    @Test
    void createsCoreTables() throws Exception {
        try (var connection = dataSource.getConnection();
             ResultSet result = connection.getMetaData()
                     .getTables(null, null, "tb_bbs_user", null)) {
            assertEquals(true, result.next());
        }
    }
}
```

- [ ] **Step 2: Run the migration test and verify RED**

```powershell
mvn.cmd -Dtest=FlywayMigrationTest test
```

Expected: FAIL because no Flyway migration exists.

- [ ] **Step 3: Replace hard-coded shared configuration**

Keep `application.properties` free of secrets:

```properties
spring.application.name=BBS
server.port=${SERVER_PORT:8080}
spring.profiles.default=local
spring.thymeleaf.cache=false
spring.thymeleaf.encoding=UTF-8
spring.thymeleaf.mode=HTML
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=30MB
mybatis-plus.configuration.map-underscore-to-camel-case=true
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when_authorized
```

Create `application-local.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/my_bbs_db?useUnicode=true&serverTimezone=UTC&characterEncoding=utf8&useSSL=false}
spring.datasource.username=${DB_USERNAME:bbs}
spring.datasource.password=${DB_PASSWORD:bbs}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=15
spring.flyway.enabled=true
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2000ms
```

Create `application-test.properties`:

```properties
spring.flyway.enabled=true
spring.thymeleaf.cache=false
spring.data.redis.repositories.enabled=false
logging.level.com.bbs.bbs.Mapper=warn
```

- [ ] **Step 4: Add the complete baseline schema migration**

Create `V1__baseline_schema.sql` with the existing core tables plus approved phase-1 account fields:

```sql
CREATE TABLE tb_bbs_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    login_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nick_name VARCHAR(30) NOT NULL,
    head_img_url VARCHAR(255) NOT NULL DEFAULT '/images/avatar/default.png',
    gender VARCHAR(10) NOT NULL DEFAULT '未知',
    location VARCHAR(50) NOT NULL DEFAULT '未知',
    introduce VARCHAR(200) NOT NULL DEFAULT '',
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    user_status TINYINT NOT NULL DEFAULT 0,
    last_login_time DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_bbs_user_login_name UNIQUE (login_name)
);

CREATE TABLE tb_bbs_post_category (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL,
    category_rank INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_bbs_category_name UNIQUE (category_name)
);

CREATE TABLE tb_bbs_post (
    post_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    publish_user_id BIGINT NOT NULL,
    post_title VARCHAR(32) NOT NULL,
    post_content MEDIUMTEXT NOT NULL,
    post_category_id INT NOT NULL,
    post_category_name VARCHAR(50) NOT NULL,
    post_status TINYINT NOT NULL DEFAULT 1,
    post_views BIGINT NOT NULL DEFAULT 0,
    post_comments BIGINT NOT NULL DEFAULT 0,
    post_collects BIGINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    last_update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_post_category_time (post_category_id, create_time),
    INDEX idx_post_status_time (post_status, is_deleted, create_time)
);

CREATE TABLE tb_bbs_post_comment (
    comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    comment_user_id BIGINT NOT NULL,
    comment_body VARCHAR(200) NOT NULL,
    parent_comment_user_id BIGINT NULL,
    comment_status TINYINT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    comment_create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_comment_post_time (post_id, comment_create_time)
);

CREATE TABLE tb_bbs_post_collect (
    collect_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_collect_user_post UNIQUE (user_id, post_id)
);

INSERT INTO tb_bbs_post_category(category_name, category_rank)
VALUES ('提问', 10), ('分享', 20), ('讨论', 30);
```

- [ ] **Step 5: Run migration and context tests**

```powershell
mvn.cmd -Dtest=FlywayMigrationTest,BbsApplicationTests test
```

Expected: both tests PASS and Flyway reports schema version `1`.

- [ ] **Step 6: Commit configuration and migration if Git is initialized**

```powershell
git add src/main/resources src/test/resources src/test/java/com/bbs/bbs/migration
git commit -m "feat: add reproducible Flyway baseline"
```

## Task 4: Introduce validated web contracts and uniform errors

**Files:**
- Create: `src/main/java/com/bbs/bbs/web/response/ApiResult.java`
- Create: `src/main/java/com/bbs/bbs/web/error/ErrorCode.java`
- Create: `src/main/java/com/bbs/bbs/web/error/BusinessException.java`
- Create: `src/main/java/com/bbs/bbs/web/error/GlobalExceptionHandler.java`
- Create: `src/main/java/com/bbs/bbs/web/filter/TraceIdFilter.java`
- Create: `src/main/java/com/bbs/bbs/web/filter/TraceIds.java`
- Create: `src/main/java/com/bbs/bbs/web/dto/auth/RegisterRequest.java`
- Create: `src/main/java/com/bbs/bbs/web/dto/auth/LoginRequest.java`
- Create: `src/main/java/com/bbs/bbs/web/dto/user/UpdateProfileRequest.java`
- Create: `src/main/java/com/bbs/bbs/web/dto/user/ChangePasswordRequest.java`
- Test: `src/test/java/com/bbs/bbs/web/error/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write a failing validation-handler test**

Use a test-only controller that receives `@Valid RegisterRequest` and assert the response has `code`, `message`, and `traceId`:

```java
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {
    @Autowired MockMvc mvc;

    @RestController
    static class TestController {
        @PostMapping("/test/register")
        ApiResult<Void> register(@Valid @RequestBody RegisterRequest request) {
            return ApiResult.success(null);
        }
    }

    @Test
    void invalidEmailReturnsStableErrorEnvelope() throws Exception {
        mvc.perform(post("/test/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginName":"bad","password":"123","nickName":"",
                         "verifyCode":"abcd"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
```

- [ ] **Step 2: Run the handler test and verify RED**

```powershell
mvn.cmd -Dtest=GlobalExceptionHandlerTest test
```

Expected: compilation FAIL because the web contract classes do not exist.

- [ ] **Step 3: Create the response and exception types**

```java
public record ApiResult<T>(String code, String message, String traceId, T data) {
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>("SUCCESS", "SUCCESS", TraceIds.current(), data);
    }

    public static ApiResult<Void> failure(String code, String message) {
        return new ApiResult<>(code, message, TraceIds.current(), null);
    }
}
```

Create the trace lookup used by `ApiResult` and the exception handler:

```java
package com.bbs.bbs.web.filter;

import org.slf4j.MDC;

public final class TraceIds {
    public static final String MDC_KEY = "traceId";

    private TraceIds() {
    }

    public static String current() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null ? "unknown" : traceId;
    }
}
```

`ErrorCode` must define exactly:

```java
public enum ErrorCode {
    VALIDATION_ERROR,
    AUTHENTICATION_REQUIRED,
    ACCESS_DENIED,
    DUPLICATE_LOGIN_NAME,
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    RESOURCE_NOT_FOUND,
    BUSINESS_ERROR,
    SYSTEM_ERROR
}
```

`BusinessException` carries an `ErrorCode` and a safe client message:

```java
public final class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

`GlobalExceptionHandler` maps validation to HTTP 400, unauthenticated to 401, forbidden to 403, missing resource to 404, business conflict to 409, and unexpected errors to 500.

- [ ] **Step 4: Create DTOs with exact validation rules**

```java
public record RegisterRequest(
        @NotBlank @Email @Size(max = 100) String loginName,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 30) String nickName,
        @NotBlank @Size(min = 4, max = 8) String verifyCode) {}

public record LoginRequest(
        @NotBlank @Email @Size(max = 100) String loginName,
        @NotBlank @Size(max = 72) String password,
        @NotBlank @Size(min = 4, max = 8) String verifyCode) {}

public record UpdateProfileRequest(
        @NotBlank @Size(max = 30) String nickName,
        @NotBlank @Pattern(regexp = "男|女|未知") String gender,
        @NotBlank @Size(max = 50) String location,
        @Size(max = 200) String introduce) {}

public record ChangePasswordRequest(
        @NotBlank @Size(max = 72) String originalPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {}
```

- [ ] **Step 5: Implement trace IDs and run tests**

Implement `TraceIdFilter` so it accepts only safe IDs and always clears MDC:

```java
package com.bbs.bbs.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-Trace-Id";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String traceId = incoming != null && SAFE.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString();
        MDC.put(TraceIds.MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIds.MDC_KEY);
        }
    }
}
```

Run:

```powershell
mvn.cmd -Dtest=GlobalExceptionHandlerTest test
```

Expected: PASS with HTTP 400 and a non-empty trace ID.

- [ ] **Step 6: Commit web contracts if Git is initialized**

```powershell
git add src/main/java/com/bbs/bbs/web src/test/java/com/bbs/bbs/web
git commit -m "feat: add validated API contracts and traceable errors"
```

## Task 5: Migrate account storage to BCrypt

**Files:**
- Modify: `src/main/java/com/bbs/bbs/bean/BBSUser.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSUserService.java`
- Modify: `src/main/java/com/bbs/bbs/service/BBSUserServiceImp.java`
- Delete after callers migrate: `src/main/java/com/bbs/util/SHA256Util.java`
- Test: `src/test/java/com/bbs/bbs/service/BBSUserServiceImpTest.java`

- [ ] **Step 1: Write failing registration and password tests**

```java
@ExtendWith(MockitoExtension.class)
class BBSUserServiceImpTest {
    @Mock BBSUserMapper userMapper;
    PasswordEncoder encoder = new BCryptPasswordEncoder(12);
    BBSUserServiceImp service;

    @BeforeEach
    void setUp() {
        service = new BBSUserServiceImp(userMapper, encoder);
    }

    @Test
    void registerStoresBCryptHash() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenReturn(1);

        service.register(new RegisterRequest(
                "user@example.com", "password123", "user", "abcd"));

        ArgumentCaptor<BBSUser> captor = ArgumentCaptor.forClass(BBSUser.class);
        verify(userMapper).insert(captor.capture());
        assertTrue(encoder.matches("password123", captor.getValue().getPasswordHash()));
        assertEquals("USER", captor.getValue().getRole());
        assertEquals((byte) 0, captor.getValue().getUserStatus());
    }
}
```

- [ ] **Step 2: Run the service test and verify RED**

```powershell
mvn.cmd -Dtest=BBSUserServiceImpTest test
```

Expected: compilation FAIL because `passwordHash`, role fields, constructor and DTO service methods do not exist.

- [ ] **Step 3: Change the user entity mapping**

Replace `passwordMd5`, add the approved role field, and keep `Byte userStatus` for phase-1 compatibility (`0 = NORMAL`, `1 = DISABLED`):

```java
@TableField("password_hash")
private String passwordHash;

private String role;
```

Keep `loginName`, `nickName`, avatar and profile fields unchanged. Add `updateTime` to match the migration.

- [ ] **Step 4: Make account service methods independent of HttpSession**

The interface must expose:

```java
void register(RegisterRequest request);
BBSUser findActiveByLoginName(String loginName);
void recordSuccessfulLogin(Long userId);
void updateProfile(Long currentUserId, UpdateProfileRequest request);
void changePassword(Long currentUserId, ChangePasswordRequest request);
```

Use constructor injection and the following method behavior. Do not write Redis in this phase; Redis user caching belongs to phase 3.

```java
public BBSUserServiceImp(BBSUserMapper userMapper,
                         PasswordEncoder passwordEncoder) {
    this.bbsUserMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
}

@Override
public void register(RegisterRequest request) {
    LambdaQueryWrapper<BBSUser> query = new LambdaQueryWrapper<>();
    query.eq(BBSUser::getLoginName, request.loginName());
    if (bbsUserMapper.selectCount(query) > 0) {
        throw new BusinessException(
                ErrorCode.DUPLICATE_LOGIN_NAME, "该邮箱已注册");
    }
    BBSUser user = new BBSUser();
    user.setLoginName(request.loginName().trim().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setNickName(request.nickName().trim());
    user.setHeadImgUrl("/images/avatar/default.png");
    user.setGender("未知");
    user.setLocation("未知");
    user.setIntroduce("");
    user.setRole("USER");
    user.setUserStatus((byte) 0);
    user.setCreateTime(new Date());
    bbsUserMapper.insert(user);
}

@Override
public BBSUser findActiveByLoginName(String loginName) {
    LambdaQueryWrapper<BBSUser> query = new LambdaQueryWrapper<>();
    query.eq(BBSUser::getLoginName, loginName.trim().toLowerCase());
    BBSUser user = bbsUserMapper.selectOne(query);
    if (user == null) {
        throw new BusinessException(
                ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
    }
    if (user.getUserStatus() == null || user.getUserStatus() != 0) {
        throw new BusinessException(
                ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
    }
    return user;
}

@Override
public void changePassword(Long userId, ChangePasswordRequest request) {
    BBSUser user = bbsUserMapper.selectById(userId);
    if (user == null || !passwordEncoder.matches(
            request.originalPassword(), user.getPasswordHash())) {
        throw new BusinessException(
                ErrorCode.INVALID_CREDENTIALS, "原密码错误");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    bbsUserMapper.updateById(user);
}
```

Add the remaining two methods:

```java
@Override
public void recordSuccessfulLogin(Long userId) {
    BBSUser patch = new BBSUser();
    patch.setUserId(userId);
    patch.setLastLoginTime(new Date());
    bbsUserMapper.updateById(patch);
}

@Override
public void updateProfile(Long userId, UpdateProfileRequest request) {
    BBSUser user = bbsUserMapper.selectById(userId);
    if (user == null) {
        throw new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
    }
    if (user.getUserStatus() == null || user.getUserStatus() != 0) {
        throw new BusinessException(
                ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
    }
    user.setNickName(request.nickName().trim());
    user.setGender(request.gender());
    user.setLocation(request.location().trim());
    user.setIntroduce(request.introduce() == null
            ? "" : request.introduce().trim());
    bbsUserMapper.updateById(user);
}
```

- [ ] **Step 5: Run account service tests**

```powershell
mvn.cmd -Dtest=BBSUserServiceImpTest test
```

Expected: PASS; captured password starts with a BCrypt prefix and does not equal the plaintext.

- [ ] **Step 6: Commit BCrypt migration if Git is initialized**

```powershell
git add src/main/java/com/bbs/bbs/bean/BBSUser.java src/main/java/com/bbs/bbs/service src/test/java/com/bbs/bbs/service/BBSUserServiceImpTest.java
git commit -m "feat: secure account passwords with BCrypt"
```

## Task 6: Replace the login interceptor with Spring Security

**Files:**
- Create: `src/main/java/com/bbs/bbs/security/BbsUserDetailsService.java`
- Create: `src/main/java/com/bbs/bbs/config/SecurityConfig.java`
- Modify: `src/main/java/com/bbs/bbs/config/MyBBSWebMvcConfigurer.java`
- Delete: `src/main/java/com/bbs/bbs/Interceptor/BBSLoginInterceptor.java`
- Test: `src/test/java/com/bbs/bbs/config/SecurityConfigTest.java`

- [ ] **Step 1: Write failing authorization tests**

```java
@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestEndpoints.class})
class SecurityConfigTest {
    @Autowired MockMvc mvc;
    @MockBean BbsUserDetailsService userDetailsService;

    @RestController
    static class TestEndpoints {
        @GetMapping("/") String home() { return "home"; }
        @PostMapping("/api/v1/me/profile") void profile() { }
        @GetMapping("/admin") String admin() { return "admin"; }
    }

    @Test
    void homepageIsPublic() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void profileWriteRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/me/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessAdmin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run security tests and verify RED**

```powershell
mvn.cmd -Dtest=SecurityConfigTest test
```

Expected: FAIL because no security chain exists and interceptor behavior does not produce API 401/403 responses.

- [ ] **Step 3: Implement database-backed UserDetailsService**

`BbsUserDetailsService` loads by email and maps status/role:

```java
return User.withUsername(user.getLoginName())
        .password(user.getPasswordHash())
        .roles(user.getRole())
        .disabled(user.getUserStatus() == null || user.getUserStatus() != 0)
        .build();
```

- [ ] **Step 4: Implement the security chain**

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index", "/posts/**", "/search",
                    "/users/**", "/login", "/register", "/common/captcha",
                    "/api/v1/auth/login", "/api/v1/auth/register",
                    "/css/**", "/js/**", "/images/**", "/layui/**").permitAll()
            .requestMatchers("/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/me/**", "/api/v1/**",
                    "/addPostPage", "/addPost", "/editPostPage/**",
                    "/editPost", "/delPost/**", "/replyPost", "/delReply/**",
                    "/addCollect/**", "/deleteCollect/**", "/uploadFile",
                    "/uploadFiles", "/updateUserInfo", "/updateHeadImg",
                    "/updatePassword", "/myCenter", "/userSet").authenticated()
            .anyRequest().permitAll())
        .formLogin(form -> form
            .loginPage("/login")
            .permitAll())
        .exceptionHandling(errors -> errors
            .defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                new AntPathRequestMatcher("/api/**")))
        .logout(logout -> logout
            .logoutUrl("/api/v1/auth/logout")
            .logoutSuccessUrl("/login")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID"));
    return http.build();
}

@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

@Bean
AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
}

@Bean
SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
}
```

Keep CSRF enabled. Thymeleaf forms and AJAX must send the generated token. The JSON login controller in Task 7 authenticates through `AuthenticationManager` and saves the resulting context through `SecurityContextRepository`; do not configure Spring's form filter to consume `/api/v1/auth/login`.

- [ ] **Step 5: Remove the legacy interceptor only after tests pass**

Delete `BBSLoginInterceptor.java` and remove `addInterceptors` plus its field from `MyBBSWebMvcConfigurer`. Retain only `/upload/**` resource mapping.

- [ ] **Step 6: Run authorization tests**

```powershell
mvn.cmd -Dtest=SecurityConfigTest test
```

Expected: public request PASS, unauthenticated API request returns 401, and non-admin request returns 403.

- [ ] **Step 7: Commit security migration if Git is initialized**

```powershell
git add src/main/java/com/bbs/bbs/config src/main/java/com/bbs/bbs/security src/main/java/com/bbs/bbs/Interceptor src/test/java/com/bbs/bbs/config
git commit -m "feat: enforce session security and roles"
```

## Task 7: Migrate authentication and profile endpoints to DTOs

**Files:**
- Modify: `src/main/java/com/bbs/bbs/handller/loginController.java`
- Modify: `src/main/java/com/bbs/bbs/handller/registerController.java`
- Modify: `src/main/java/com/bbs/bbs/handller/BBSUserController.java`
- Modify: `src/main/resources/templates/user/login.html`
- Modify: `src/main/resources/templates/user/reg.html`
- Modify: `src/main/resources/templates/user/set.html`
- Modify: `src/main/resources/templates/header.html`
- Test: `src/test/java/com/bbs/bbs/handller/AuthenticationControllerTest.java`
- Test: `src/test/java/com/bbs/bbs/handller/UserProfileControllerTest.java`

- [ ] **Step 1: Write failing controller validation and identity tests**

Add MockMvc tests proving invalid email returns 400, duplicate email returns 409, anonymous profile update returns 401, and the profile service receives the authenticated principal's ID rather than a request-supplied ID.

```java
mvc.perform(post("/api/v1/auth/register")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"loginName":"bad","password":"password123",
                 "nickName":"tester","verifyCode":"abcd"}
                """))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
```

- [ ] **Step 2: Run controller tests and verify RED**

```powershell
mvn.cmd -Dtest=AuthenticationControllerTest,UserProfileControllerTest test
```

Expected: FAIL because the `/api/v1` DTO endpoints do not exist.

- [ ] **Step 3: Keep GET page methods and replace write methods**

Required controller signatures:

```java
@PostMapping("/api/v1/auth/login")
@ResponseBody
public ApiResult<Void> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse)

@PostMapping("/api/v1/auth/register")
@ResponseBody
public ApiResult<Void> register(@Valid @RequestBody RegisterRequest request,
                                HttpSession session)

@PutMapping("/api/v1/me/profile")
@ResponseBody
public ApiResult<Void> updateProfile(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody UpdateProfileRequest request)

@PutMapping("/api/v1/me/password")
@ResponseBody
public ApiResult<Void> changePassword(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody ChangePasswordRequest request,
        HttpServletRequest servletRequest)
```

The JSON login implementation must authenticate through Spring Security and persist the context:

```java
verifyCaptcha(servletRequest.getSession(), request.verifyCode());
Authentication authentication = authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(
                request.loginName(), request.password()));
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
securityContextRepository.saveContext(
        context, servletRequest, servletResponse);

BBSUser user = bbsUserService.findActiveByLoginName(request.loginName());
servletRequest.getSession(true).setAttribute(Constants.USER_SESSION_KEY, user);
bbsUserService.recordSuccessfulLogin(user.getUserId());
servletRequest.getSession().removeAttribute(Constants.VERIFY_CODE_KEY);
return ApiResult.success(null);
```

Use the same exact captcha comparison for registration:

```java
private void verifyCaptcha(HttpSession session, String submittedCode) {
    Object stored = session.getAttribute(Constants.VERIFY_CODE_KEY);
    if (!(stored instanceof String expected)
            || !expected.equalsIgnoreCase(submittedCode)) {
        throw new BusinessException(
                ErrorCode.VALIDATION_ERROR, "验证码错误");
    }
}
```

The `Constants.USER_SESSION_KEY` assignment is a temporary compatibility bridge for legacy post/comment controllers; Spring Security remains the authority for route access. Resolve profile user IDs by principal email in the service. After a password change, invalidate the current Session and require login again.

- [ ] **Step 4: Update forms and AJAX for CSRF and new routes**

Every Thymeleaf page that performs AJAX must expose:

```html
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.headerName}">
```

The shared AJAX setup must send the header:

```javascript
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
if (csrfToken && csrfHeader) {
  $.ajaxSetup({headers: {[csrfHeader]: csrfToken}});
}
```

- [ ] **Step 5: Run authentication/profile controller tests**

```powershell
mvn.cmd -Dtest=AuthenticationControllerTest,UserProfileControllerTest test
```

Expected: all tests PASS; no controller accepts a `userId` for self-service profile operations.

- [ ] **Step 6: Commit controller migration if Git is initialized**

```powershell
git add src/main/java/com/bbs/bbs/handller src/main/resources/templates src/main/resources/static/js src/test/java/com/bbs/bbs/handller
git commit -m "refactor: validate account and profile requests"
```

## Task 8: Complete phase-1 verification

**Files:**
- Modify if needed: `HELP.md`
- Create: `.env.example`
- Test: all phase-1 tests

- [ ] **Step 1: Add safe local environment documentation**

Create `.env.example`:

```dotenv
DB_URL=jdbc:mysql://localhost:3306/my_bbs_db?useUnicode=true&serverTimezone=UTC&characterEncoding=utf8&useSSL=false
DB_USERNAME=bbs
DB_PASSWORD=change-me
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
SERVER_PORT=8080
```

Update `HELP.md` with the exact Maven test command, local environment variables, database creation prerequisite, and `mvn.cmd spring-boot:run -Dspring-boot.run.profiles=local`.

- [ ] **Step 2: Run all unit and web tests**

```powershell
mvn.cmd test
```

Expected: `BUILD SUCCESS`, zero failures and zero errors.

- [ ] **Step 3: Run the full verification lifecycle**

```powershell
mvn.cmd verify
```

Expected: Flyway/Testcontainers integration tests pass and Maven exits 0.

- [ ] **Step 4: Start the application against an empty local database**

```powershell
$env:DB_USERNAME='bbs'
$env:DB_PASSWORD='bbs'
mvn.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Expected logs: Flyway applies version 1, Spring Security filter chain initializes, and the application listens on port 8080 without schema or bean errors.

- [ ] **Step 5: Perform the phase-1 smoke checks**

Verify:

```text
GET  /                       -> 200 public page
GET  /login                  -> 200
POST /api/v1/auth/register   -> success with valid CSRF/captcha
POST /api/v1/auth/login      -> authenticated Session
GET  /me                     -> authenticated page
GET  /admin                  -> 403 for USER
PUT  /api/v1/me/profile      -> ignores any client user ID
PUT  /api/v1/me/password     -> BCrypt update and Session invalidation
```

- [ ] **Step 6: Commit phase-1 completion if Git is initialized**

```powershell
git add .env.example HELP.md pom.xml src
git commit -m "feat: complete secure reproducible BBS baseline"
```

## Follow-on plans

Do not merge the following subsystems into phase 1. Create a separate detailed plan for each after phase 1 passes:

1. `bbs-phase-2-core-forum.md`: normalized post/comment/collect DTO APIs, state model, ownership and admin moderation.
2. `bbs-phase-3-redis-hot-ranking.md`: Cache-Aside, TTL jitter, null caching, view dedup, rate limiting and ZSET heat ranking.
3. `bbs-phase-4-outbox-rocketmq-notifications.md`: Outbox publisher, idempotent consumers, notification table/API and daily statistics.
4. `bbs-phase-5-elasticsearch-search.md`: index mapping, IK analyzer, search/highlight/filter/sort, rebuild and reconciliation.
5. `bbs-phase-6-rag.md`: retrieval, context budget, prompt guardrails, OpenAI-compatible adapter, citations and refusal behavior.
6. `bbs-phase-7-delivery-quality.md`: Docker Compose, failure injection, full E2E chain and performance verification.
