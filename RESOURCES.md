# Linux and Docker Learning Through BBS Resources

## Knowledge

- [Red Hat: CentOS Linux end of life](https://www.redhat.com/en/topics/linux/centos-linux-eol)
  Vendor-maintained explanation of the CentOS Linux lifecycle and migration context. Use for: understanding why CentOS 7 is acceptable for an isolated lab but not a new production baseline.
- [Red Hat Enterprise Linux 7 documentation](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/7)
  Primary administration references closest to CentOS 7, including services, users, permissions, networking, firewalld and SELinux. Use for: checking host-level commands and behavior.
- [Docker CLI reference](https://docs.docker.com/reference/cli/docker/)
  Official syntax and options for Docker commands. Use for: verifying command flags rather than relying on copied examples.
- [Docker storage volumes](https://docs.docker.com/engine/storage/volumes/)
  Official volume lifecycle and persistence model. Use for: keeping MySQL and Elasticsearch data when containers are replaced.
- [Docker bridge networks](https://docs.docker.com/engine/network/drivers/bridge/)
  Official user-defined bridge behavior and container name resolution. Use for: connecting BBS to MySQL, Redis and other services without hard-coded container IPs.
- [Docker container logs](https://docs.docker.com/reference/cli/docker/container/logs/)
  Official `docker logs` options. Use for: diagnosing containers that exit, fail health checks or reject connections.
- [Docker Compose documentation](https://docs.docker.com/compose/)
  Official multi-container application model and CLI guidance. Use for: the final repeatable BBS deployment after each service is understood separately.
- [Testcontainers: Supported Docker environments](https://java.testcontainers.org/supported_docker_environment/)
  Official compatibility and Docker-host guidance. Use for: deciding where Maven and Docker should run.
- [Testcontainers: MySQL module](https://java.testcontainers.org/modules/databases/mysql/)
  Official MySQL container examples. Use for: understanding `MySQLContainer` lifecycle and JDBC properties.
- [Docker Engine installation](https://docs.docker.com/engine/install/)
  Official Linux installation and service instructions. Use for: preparing the VM.
- [Docker: Protect the Docker daemon socket](https://docs.docker.com/engine/security/protect-access/)
  Official TLS and SSH security guidance. Use for: remote Docker access; never expose unauthenticated port 2375.
- [Flyway migrations](https://documentation.red-gate.com/flyway/reference/usage/concepts/migrations)
  Official migration naming and execution model. Use for: understanding `V1__baseline_schema.sql`.

## Wisdom (Communities)

- [Testcontainers GitHub Discussions](https://github.com/testcontainers/testcontainers-java/discussions)
  Use for: environment-specific remote Docker and networking failures after checking official documentation.
- [Docker Community Forums](https://forums.docker.com/)
  Moderated product community. Use for: environment-specific Docker Engine and Compose questions after reproducing the issue with official diagnostics.

## Gaps

- CentOS 7 is end-of-life, so current package repository behavior may differ by mirror and VM history. Before changing repository files, capture `yum repolist` and the first complete repository error for review.
