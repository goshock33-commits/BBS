# Mission: Learn Linux and Docker by Deploying BBS

## Why
Use the existing CentOS 7 virtual machine and the BBS project as a practical path to learn Linux administration and Docker. Start with the Task 3 MySQL migration test, then progress toward a reproducible multi-service BBS deployment.

## Success looks like
- Navigate Linux, inspect files, permissions, processes, services, ports and logs without relying on copied commands blindly.
- Manage Docker images, containers, networks, volumes, ports and logs, including safe cleanup and troubleshooting.
- Run the Task 3 Flyway tests against a disposable MySQL 8.4 container.
- Deploy the BBS application and its required infrastructure incrementally, then operate the stack with Docker Compose.
- Explain where application state lives and recover a failed service without losing database data.

## Constraints
- Project development remains on Windows.
- The learning server is a CentOS 7 virtual machine with Docker Engine already installed.
- CentOS 7 is end-of-life, so it is treated as an isolated learning environment rather than a production baseline.
- Lessons use small, observable exercises and Chinese explanations suitable for a Linux and Docker beginner.
- Do not expose an unauthenticated Docker daemon over TCP.

## Out of scope
- Public internet production deployment, high availability and Kubernetes.
- Operating-system upgrade or migration until the beginner deployment path is understood.
- Production-grade secrets management and cluster operations.
