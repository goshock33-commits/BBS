# BBS Task 3 Docker Resources

## Knowledge

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
