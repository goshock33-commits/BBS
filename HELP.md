# BBS 本地运行与验证

## 环境要求

- Java 17 或更高版本
- Maven 3.9 或使用项目中的 Maven Wrapper
- MySQL 8.x
- Redis 6.x 或更高版本
- 完整集成验证需要 Docker；Linux 与 Docker 命令由项目学习者执行

## 本地数据库

先创建数据库和最小权限账号：

```sql
CREATE DATABASE my_bbs_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'bbs'@'%' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON my_bbs_db.* TO 'bbs'@'%';
FLUSH PRIVILEGES;
```

不要把真实密码提交到 Git。`.env.example` 仅提供变量名和示例值；PowerShell 中可这样设置当前终端：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/my_bbs_db?useUnicode=true&serverTimezone=UTC&characterEncoding=utf8&useSSL=false'
$env:DB_USERNAME='bbs'
$env:DB_PASSWORD='change-me'
$env:REDIS_HOST='localhost'
$env:REDIS_PORT='6379'
$env:REDIS_PASSWORD=''
$env:SERVER_PORT='8080'
```

## 启动应用

```powershell
mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

启动成功后访问 `http://localhost:8080/`。Flyway 会在空数据库中创建 Phase 1 表结构。

## Windows 非 Docker 回归

以下命令排除必须使用 Docker 的 MySQL Testcontainers 测试：

```powershell
mvn.cmd "-Dtest=!FlywayMigrationTest,!BbsApplicationTests" test
mvn.cmd -DskipTests compile
```

## Linux Docker 集成验证

完整 Phase 1 验证不能由上面的 Windows 回归替代。请在 CentOS VM 中按教程操作：

- `lessons/0001-run-task3-with-linux-vm-docker.html`

关键 Maven 命令：

```bash
mvn -Dtest=FlywayMigrationTest,BbsApplicationTests test
```

只有该命令在可用的 Docker 环境中通过后，才能将 Task 3 标记完成并继续关闭 Task 8。
