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
$env:DB_URL='jdbc:mysql://localhost:3306/my_bbs_db?useUnicode=true&serverTimezone=UTC&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='bbs'
$env:DB_PASSWORD='change-me'
$env:REDIS_HOST='localhost'
$env:REDIS_PORT='6379'
$env:REDIS_PASSWORD=''
$env:SERVER_PORT='8080'
$env:BBS_UPLOAD_DIR='D:\upload'
```

`allowPublicKeyRetrieval=true` 仅用于关闭 TLS 的本地 MySQL 8 开发连接；生产环境应启用 TLS。上传目录必须是运行应用的用户可写目录，未设置 `BBS_UPLOAD_DIR` 时默认使用当前项目下的 `upload` 目录。

## 启动应用

```powershell
mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

启动成功后访问 `http://localhost:8080/`。Flyway 会按顺序执行 V1 和 V2；
V2 会保留已有帖子、评论和收藏数据，并补充内容状态、乐观锁版本、
分类启用状态、收藏唯一约束和管理员审核记录表。

CentOS 本地运行可使用独立上传目录：

```bash
export BBS_UPLOAD_DIR='/opt/image/upload'
```

请先确保启动应用的用户对该目录有写权限；也可以省略该变量，使用项目目录下的 `upload`。

## Windows 非 Docker 回归

以下命令排除必须使用 Docker 的 MySQL Testcontainers 测试：

```powershell
mvn.cmd "-Dtest=!FlywayMigrationTest,!FlywayCoreForumMigrationTest,!BbsApplicationTests" test
mvn.cmd -DskipTests compile
git diff --check
```

## Phase 2 页面与接口

规范论坛接口位于 `/api/v1`，包括帖子、评论、回复、收藏和启用分类。
写操作需要登录和 CSRF Token，当前操作用户由 Spring Security Session 确定，
请求体不接受当前操作用户 ID。

管理员登录后可通过以下页面执行基础治理：

- `/admin`：管理首页和总量统计
- `/admin/posts`：帖子筛选、隐藏、恢复和删除
- `/admin/comments`：评论筛选、隐藏、恢复和删除
- `/admin/users`：用户禁用和恢复
- `/admin/categories`：分类新增、编辑、启用和停用

普通用户访问 `/admin/**` 或 `/api/v1/admin/**` 返回 403。隐藏和删除操作
必须填写原因，并记录管理员、目标、动作、原因和时间。

## Linux Docker 集成验证

完整 Phase 2 验证不能由上面的 Windows 回归替代。CentOS 项目目录为
`/root/BBS1`，分支为 `phase1/baseline-security`。Phase 1 的环境准备仍可参考：

- `lessons/0001-run-task3-with-linux-vm-docker.html`

关键 Maven 命令：

```bash
mvn verify
mvn -Dtest=FlywayMigrationTest,FlywayCoreForumMigrationTest,BbsApplicationTests test
```

`FlywayCoreForumMigrationTest` 同时验证空库迁移和带有旧数据的 V1 -> V2 迁移。
只有完整验证、空库迁移和普通用户/管理员浏览器流程都通过后，才能关闭
Phase 2 Task 8。
