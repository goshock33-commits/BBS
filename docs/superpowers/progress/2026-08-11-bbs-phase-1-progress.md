# BBS Phase 1 Progress

## Session

- Date: 2026-08-11
- Goal: execute Phase 1 baseline/security implementation
- Worktree: `.worktrees/phase1-baseline-security`
- Branch: `phase1/baseline-security`
- Base commit: `4466988 chore: establish BBS project baseline`
- Workflow: fresh implementer -> spec-compliance review -> code-quality review -> fix/re-review

## Approved scope

- Spring Boot monolith with Thymeleaf/Layui/jQuery
- MySQL source of truth, Redis cache/hot list
- RocketMQ/ElasticSearch/RAG remain follow-on work unless explicitly included by a later phase
- Phase 1 priorities: compile baseline, Flyway/configuration, DTO + Bean Validation, uniform errors, BCrypt, Spring Security, auth/profile endpoint migration, verification

## Task status

- [x] Task 1: establish failing baseline and dependency set
- [x] Task 2: repair homepage pagination with tests
- [ ] Task 3: add environment-backed configuration and Flyway baseline (integration verification pending Docker)
- [x] Task 4: introduce validated web contracts and uniform errors
- [x] Task 5: migrate account storage to BCrypt
- [x] Task 6: replace login interceptor with Spring Security
- [x] Task 7: migrate authentication and profile endpoints to DTOs
- [ ] Task 8: complete Phase 1 verification

## Review log

### Task 1

- Baseline compiler reproduced the expected single source error: `BBSPostServiceImpl` does not implement `getBBSPostPageForIndex(PageQueryUtil)`.
- Implementation commit: `df155e3 build: add baseline validation and security dependencies`.
- First spec review: rejected because `MySqlContainerSupport` used `root/root` instead of the required `bbs/bbs` test credentials.
- Spec correction commit: `e73cfb8 test: align baseline database credentials`.
- Post-correction compiler result: expected single source error remains; dependency resolution succeeds.
- Controller spec re-review confirmed database, username and password are all `bbs`; dependency scopes and context-test wiring remain compliant.
- Controller quality review found no additional Task 1 issues. Independent re-review could not be dispatched because the subagent entry point was unavailable.
- Status: complete.

### Task 2

- RED: focused test failed because `getBBSPostPageForIndex(PageQueryUtil)` was absent.
- Implementation commit: `767287e fix: restore homepage post pagination`.
- Implemented active/non-deleted filters, category/keyword/period filters, views/time ordering, page-size cap, batch author enrichment and non-null empty pages.
- Verification: `BBSPostServiceImplTest` 2/2 passed; `mvn.cmd -DskipTests compile` returned `BUILD SUCCESS`.
- A proposed SQL-segment assertion failed because a pure Mockito test does not initialize MyBatis-Plus lambda metadata; it was removed instead of adding test-only production coupling.
- Controller spec and quality review found no blocking issue. Independent reviewers could not be dispatched because the subagent entry point was unavailable.
- Status: complete.

### Task 3

- RED test added: `FlywayMigrationTest.createsCoreTables`.
- Configuration and migration files added; collection primary key uses existing entity-compatible `record_id`.
- `mvn.cmd -DskipTests compile` and `mvn.cmd -DskipTests test-compile` both returned `BUILD SUCCESS`.
- Integration test was attempted with a sanitized child-process PATH; it reached Testcontainers and failed because no valid Docker environment is installed. The original environment also contains malformed quoted PATH entries, which were isolated from the project.
- Linux VM verification tutorial: `lessons/0001-run-task3-with-linux-vm-docker.html`. Recommended execution runs Maven and Docker together inside the VM; remote Docker over TCP is documented only as a TLS-protected advanced option.
- Status: implementation complete, integration verification pending Docker; do not claim the migration has run successfully until the container test passes.

### Task 4

- RED: `GlobalExceptionHandlerTest` initially failed because web contracts were absent.
- Added `spring-boot-starter-webmvc-test`; Spring Boot 4 moved MVC test slices out of the base test starter and changed the annotation package.
- Added `ApiResult<T>`, generic `PageResult<T>`, stable `ErrorCode`, `BusinessException`, global exception mapping, trace ID support and four validated request DTOs.
- Isolated the MVC slice from the application's global `@MapperScan` using an explicit test context.
- Verification: `GlobalExceptionHandlerTest` and `BBSPostServiceImplTest` ran 3 tests with 0 failures and 0 errors.
- Status: complete; independent reviewers remain unavailable in this session, so controller review is recorded instead.

### Task 5

- RED: account service tests initially failed because the BCrypt constructor, DTO methods, `passwordHash` and `role` mapping were absent.
- Added a single BCrypt encoder bean with cost 12; registration, login and password changes now use plaintext-to-BCrypt verification without SHA-256.
- Mapped `password_hash`, `role` and `update_time`; password hashes are excluded from `toString` and Jackson/Redis JSON serialization.
- Review RED: 5 focused failures reproduced Redis JSON hash exposure, zero-row update success, legacy password-update false positives and the BCrypt 72-byte UTF-8 boundary.
- Review fixes: account reads and writes no longer use Redis in Phase 1, profile/password updates require a successful database write, and passwords over 72 UTF-8 bytes return `VALIDATION_ERROR` before BCrypt encoding.
- Verification: 17 non-Docker regression tests passed across account services, password configuration, user serialization, homepage pagination and global errors; compilation and final diff checks are recorded at the Task 5 commit boundary.
- Existing SHA-256 hashes cannot be converted to BCrypt because hashes are irreversible; existing accounts require a later reset or login-time migration policy.
- Status: complete. Task 3 remains pending learner-run Linux Docker verification and still gates final Task 8 verification.

### Task 6

- RED: authorization tests initially failed because `SecurityConfig` and `BbsUserDetailsService` did not exist.
- Added database-backed Spring Security users with normalized email, BCrypt hash, `USER`/`ADMIN` authorities and disabled-account mapping.
- Added a session-based security chain: public browsing/authentication routes, authenticated personal and write routes, and `ADMIN`-only management routes.
- All non-authentication `/api/v1/**` routes require a logged-in user; anonymous APIs return 401, protected pages redirect to `/login`, and role failures return 403.
- CSRF remains enabled. A review RED test proved a second form-login processing URL could bypass the project captcha, so Spring's form filter was removed; Task 7 will authenticate the JSON captcha flow through `AuthenticationManager`.
- Deleted `BBSLoginInterceptor` and removed MVC interceptor registration; the upload resource mapping remains.
- Spring Security 7 uses `PathPatternRequestMatcher`; the removed `AntPathRequestMatcher` API from the original plan was not used.
- Verification: 8 focused security tests passed. Full non-Docker regression, compilation and final diff checks are recorded at the Task 6 commit boundary.
- Status: complete; 25 non-Docker regression tests and compilation passed before commit.

### Task 7

- RED: controller tests failed because the `/api/v1` DTO endpoints did not exist and registration still depended on `BBSUserServiceImp` directly.
- Replaced the four legacy account write routes with JSON DTO endpoints for registration, login, profile updates and password changes; page GET routes and the legacy avatar update bridge remain compatible.
- Registration and login verify the Session captcha case-insensitively and remove it only after success. Login authenticates through `AuthenticationManager`, changes the Session ID, persists the Spring Security context and then stores the temporary legacy user Session object.
- Self-service profile and password operations resolve the user ID from the authenticated principal email; request-supplied IDs are ignored. Profile changes refresh the legacy Session user, while password changes invalidate the Session and require login again.
- Removed state-changing `GET /logout`; logout now uses the Spring Security POST route with CSRF protection.
- Updated login, registration and settings pages to send JSON to the new routes. The shared header supplies CSRF metadata and jQuery headers; avatar upload uses Layui upload headers instead of placing the token in the URL.
- Review fixes aligned the browser password rule with the 8-72 character and 72 UTF-8 byte backend contract, prevented partially persisted authentication when compatibility user lookup fails, and added Session fixation protection.
- Verification: 34 non-Docker tests passed with zero failures and errors; compilation and `git diff --check` passed. Docker-backed Flyway/context verification is still pending under Task 3.
- Status: complete.

### Task 8

- Added `.env.example` with local database, Redis and server variables; no real credentials are stored in Git.
- Added `HELP.md` with MySQL bootstrap SQL, PowerShell environment setup, local startup, non-Docker regression commands and the Linux Docker integration handoff.
- Windows verification available so far: 34 non-Docker tests and compilation pass; `git diff --check` reports no errors.
- Remaining: run `FlywayMigrationTest` and `BbsApplicationTests` in the learner-managed Linux Docker environment, then start against an empty MySQL schema and perform the Phase 1 smoke checks.
- Status: in progress; Task 3 Docker verification still gates completion.

## Handoff notes

- Read the full approved spec at `docs/superpowers/specs/2026-08-11-bbs-requirements-design.md`.
- Read the full implementation plan at `docs/superpowers/plans/2026-08-11-bbs-phase-1-baseline-security.md`.
- Source investigation must use CodeGraph before inspecting project source files.
- Do not implement directly on `main`; work only in this worktree.
- Do not dispatch parallel implementers; each task uses a fresh implementer and sequential reviews.
- The PowerShell profile currently prints a Conda `Invoke-Expression` parse error after commands; rely on the command exit code and primary output because Git/Maven commands themselves still execute.
