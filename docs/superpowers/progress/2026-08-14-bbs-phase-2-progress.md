# BBS Phase 2 Progress

## Session

- Date: 2026-08-14
- Goal: execute Phase 2 core forum implementation
- Worktree: `.worktrees/phase1-baseline-security`
- Branch: `phase1/baseline-security`
- Phase 1 completion commit: `0979922 docs: close Phase 1 verification`
- Phase 2 plan commit: `b8e5b88 docs: plan Phase 2 core forum`
- Workflow: task-by-task TDD, regression, review and commit

## Scope

- Included: V2 migration, forum DTO/API contracts, post/category/comment/collection
  services, administrator moderation APIs and minimum Thymeleaf/Layui pages.
- Deferred: Phase 3 Redis, Phase 4 Outbox/RocketMQ, Phase 5 Elasticsearch,
  Phase 6 RAG and Phase 7 Compose/E2E/performance work.
- `main` remains unmerged; all work is on `phase1/baseline-security`.

## Task Status

- [x] Task 1: establish Phase 2 schema migration
- [x] Task 2: define DTOs, enums and stable errors
- [x] Task 3: normalize post and category services
- [x] Task 4: normalize comment and collection services
- [x] Task 5: add canonical `/api/v1` forum APIs
- [x] Task 6: implement administrator moderation APIs
- [x] Task 7: integrate existing Thymeleaf/Layui pages
- [ ] Task 8: complete Phase 2 verification and handoff

## Commit Log

- `2c67a60 feat: add Phase 2 forum state schema`
- `6e070d3 feat: define core forum DTO contracts`
- `b5691a9 feat: normalize post and category services`
- `a9d80f6 feat: enforce forum social ownership and counters`
- `9799794 feat: expose normalized forum APIs`
- `a6daa11 feat: add administrator moderation APIs`
- `3269d93 feat: add core forum and admin pages`
- `3cc44bb fix: restore Phase 2 homepage rendering`

## Implemented Behavior

- V2 preserves V1 data while adding explicit content state, optimistic-lock
  versions, category enabled state, collection uniqueness and
  `moderation_record`.
- Public post/comment/collection views exclude `HIDDEN` and `DELETED` data.
  Authors can see their own hidden posts in the personal center without
  increasing views or writing a post cache entry.
- Owner-only post edits/deletes, comment ownership/post-owner deletion,
  disabled-category rejection and guarded non-negative counters are enforced
  in services.
- Collection and cancellation are idempotent; repeated collection cannot
  increase the aggregate count twice.
- Canonical forum and administrator JSON APIs use DTO validation,
  `ApiResult`/`PageResult`, CSRF and SecurityContext-derived identity.
- Administrator pages cover totals, post/comment governance, user state and
  category management. Hidden/delete actions require reasons and create audit
  records.
- Legacy page routes remain as compatibility bridges, with writes migrated to
  `/api/v1`.

## Review Fixes

- Hidden author detail no longer increments views or writes the post cache.
- Public user pages and collection/recent-comment lists re-check post state
  from MySQL and exclude non-public content.
- Recent comments return an empty list when all associated posts are removed.
- Post governance safely renders escaped content directly in the admin page.
- Category management loads both enabled and disabled categories.
- Browser acceptance exposed a homepage failure after the first recent post:
  the legacy hot-topic path entered Redis only when its result was nonempty.
  Phase 2 hot-topic rendering no longer reads or writes Redis.
- Missing JavaScript/font resources now return an empty page-resource 404
  instead of passing through the JSON `ApiResult` exception handler with an
  incompatible preset content type.

## Windows Verification

Fresh verification on 2026-08-14 after Task 7:

- Non-Docker Maven regression: 104 tests, 0 failures, 0 errors, 0 skipped;
  `BUILD SUCCESS`.
- Compilation: `mvn.cmd -DskipTests compile` returned `BUILD SUCCESS`.
- JavaScript syntax: `node --check src/main/resources/static/js/admin.js`
  exited 0.
- Patch checks: `git diff --check` and Task 7 staged diff check exited 0;
  only Git LF/CRLF conversion warnings were printed.

## Pending Linux And Browser Evidence

The following Task 8 evidence must be supplied from CentOS `/root/BBS1` by
the project owner and must not be inferred from Windows results:

- Complete `mvn verify` with Docker available.
- Flyway V1/V2 migration against an empty MySQL database.
- V1 -> V2 migration with existing posts, comments and collections preserved.
- Guest/public visibility checks for `NORMAL`, `HIDDEN` and `DELETED` content.
- Ordinary USER create/edit/delete/comment/collect flows and 403 admin access.
- ADMIN post/comment moderation, user status and category management flows.
- Audit verification for moderation operator, target, action, reason and time.
- Legacy public and personal pages still render.

Task 8 remains open until this evidence is recorded. Do not merge `main`.
