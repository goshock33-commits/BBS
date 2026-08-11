# Mission: Verify BBS Task 3 with Linux VM Docker

## Why
Use the existing Linux virtual machine and Docker Engine to run the BBS project's real MySQL 8.4 migration tests, so the Flyway baseline is proven against MySQL rather than only compiling on Windows.

## Success looks like
- Run `FlywayMigrationTest` and `BbsApplicationTests` against a disposable MySQL 8.4 container.
- See Flyway apply schema version 1 and Maven report zero failures.
- Understand that Testcontainers creates and removes the database container automatically.

## Constraints
- Project development remains on Windows.
- Docker Engine is available inside a Linux virtual machine.
- Do not expose an unauthenticated Docker daemon over TCP.

## Out of scope
- Production deployment and Docker Compose for the complete BBS stack.
- Elasticsearch, RocketMQ, Redis and RAG containers.
