# Authenticated Post Views Design

## Scope

- Protect only the legacy HTML post detail route `/detail/{postId}`.
- Redirect anonymous page requests to `/login` through Spring Security.
- Keep `GET /api/v1/posts/{id}` public and unchanged.
- Count at most one view for each authenticated user and post pair.
- Do not introduce or depend on Redis in Phase 2.

## Persistence

Add a V3 Flyway migration that creates `tb_bbs_post_view` with:

- an auto-increment primary key;
- `post_id` and `user_id` foreign-key identifiers;
- a creation timestamp;
- a unique constraint on `(post_id, user_id)`;
- indexes that support lookup by user and post.

V3 is required because V2 may already have been applied. Existing migration files must not be changed.

## Request Flow

1. Spring Security rejects an anonymous `/detail/**` request before the controller and redirects it to `/login`.
2. An authenticated request loads the post and author from MySQL.
3. `HIDDEN` remains visible only to its author and does not gain views. `DELETED` remains unavailable.
4. For a `NORMAL` post, insert the `(post_id, user_id)` view record with duplicate-safe SQL.
5. Increase `post_views` with atomic SQL only when the insert reports a newly created record.
6. A duplicate visit or refresh returns the detail page without changing the counter.

The unique database constraint is the concurrency boundary: simultaneous first visits by the same account cannot both increase the counter.

## Failure Handling

- A missing or unavailable post continues to render the existing 404 page.
- A duplicate view is a normal idempotent outcome, not an error.
- A database failure while recording a genuinely new view fails the request instead of silently producing inconsistent counters.

## Verification

- Security test: anonymous `/detail/{postId}` redirects to `/login`.
- Security test: authenticated access reaches the protected route.
- Service tests: first visit inserts a record and increments once; repeated visit does not increment; different users can each increment once; hidden author access does not record a view.
- Migration script test: V3 contains the table and unique constraint.
- MySQL migration test: V1 through V3 apply on an empty database and preserve existing data.
- Run focused tests RED then GREEN, the Windows non-Docker regression suite, compilation, and `git diff --check`.
