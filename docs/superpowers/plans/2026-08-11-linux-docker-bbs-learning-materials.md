# Linux and Docker BBS Learning Materials Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a CentOS 7-oriented Linux and Docker command reference plus a guided learning path that teaches a beginner by progressively deploying and operating the BBS project.

**Architecture:** Keep every teaching artifact self-contained and browser-readable. Use `MISSION.md` and `RESOURCES.md` as the durable teaching context, place printable reference pages under `reference/`, keep hands-on units under `lessons/`, and record observable progress in the existing Phase 1 progress document.

**Tech Stack:** Static HTML5/CSS, Mermaid diagrams where rendered by Codex, CentOS 7 shell commands, Docker Engine CLI, Testcontainers, Maven, Git, Markdown.

---

### Task 1: Curate authoritative sources and learner context

**Files:**
- Modify: `RESOURCES.md`
- Create: `NOTES.md`

**Step 1: Add primary Linux and Docker sources**

Add annotated links for:

- CentOS Linux 7 end-of-life information.
- Red Hat Enterprise Linux 7 system administration documentation for `systemctl`, `journalctl`, users, permissions, networking, `firewalld` and SELinux concepts.
- Docker CLI reference, storage volumes, bridge networks, container logs and Compose.
- Existing Testcontainers, MySQL and Flyway sources.

Every entry must state what it covers and when the learner should use it. Do not add unverified blog tutorials as authoritative sources.

**Step 2: Record learner preferences**

Create `NOTES.md` with these established facts:

```markdown
# Teaching Notes

- The learner uses a CentOS 7 virtual machine with Docker already installed.
- The learner identifies as a Linux and Docker beginner.
- Teach in Chinese through small BBS deployment challenges.
- Explain command effects, expected output and recovery before destructive operations.
- Prefer visual flows and printable command references.
```

**Step 3: Verify resource coverage**

Run:

```powershell
rg -n "CentOS|Red Hat|Docker CLI|volume|network|Compose|Testcontainers|Flyway" RESOURCES.md NOTES.md
```

Expected: every required subject appears at least once and every resource has an explanatory line.

**Step 4: Commit**

```powershell
git add RESOURCES.md NOTES.md
git commit -m "docs: curate Linux Docker learning sources"
```

### Task 2: Create the Linux and Docker command reference

**Files:**
- Create: `reference/0001-centos7-linux-docker-command-reference.html`

**Step 1: Build the document shell**

Create a self-contained Chinese HTML5 page with:

- Responsive desktop and mobile layout.
- Print styles with high contrast and no clipped code blocks.
- A sticky or compact table of contents that collapses safely on mobile.
- Four visible risk labels: `只读检查`, `改变状态`, `删除资源`, `需要 root`.
- A visual explanation of `命令 + 选项 + 参数` and common shell prompt symbols.
- Links to `../lessons/0002-centos7-first-docker-container.html` and `0002-bbs-docker-learning-roadmap.html`.

Avoid decorative gradients, nested cards and oversized headings. Use tables only where commands need side-by-side comparison.

**Step 2: Add CentOS 7 Linux commands**

Cover these sections with purpose, syntax, a BBS example, expected result and the most important warning:

- Help and history: `man`, `--help`, `history`, `clear`.
- Identity and system: `whoami`, `id`, `hostnamectl`, `cat /etc/os-release`, `uname -r`, `date`.
- Navigation and files: `pwd`, `ls`, `cd`, `mkdir`, `touch`, `cp`, `mv`, `rm`, `find`.
- Reading and search: `cat`, `less`, `head`, `tail -f`, `grep`.
- Permissions: `ls -l`, `chmod`, `chown`, `sudo`; explain owner/group/other and numeric permission notation.
- Processes and services: `ps`, `top`, `kill`, `systemctl`, `journalctl`.
- Resources: `free -h`, `df -h`, `du -sh`, `lsblk`.
- Networking: `ip addr`, `ss -lntp`, `curl`, `ping`, `firewall-cmd`.
- Packages and archives: `yum`, `rpm`, `tar`, `gzip`.
- SELinux inspection: `getenforce`, `ls -Z`; do not recommend disabling SELinux as a default fix.

Use explicit paths and named files for deletion examples. Do not include broad recursive deletion commands.

**Step 3: Add Docker commands**

Cover:

- Environment: `docker version`, `docker info`, `docker system df`.
- Images: `docker pull`, `docker images`, `docker image inspect`, `docker rmi`.
- Containers: `docker run`, `docker ps`, `docker ps -a`, `docker stop`, `docker start`, `docker restart`, `docker rm`.
- Observation: `docker logs`, `docker inspect`, `docker stats`, `docker port`, `docker top`.
- Interaction: `docker exec`, `docker cp`.
- Storage: `docker volume create`, `ls`, `inspect`, `rm`.
- Networking: `docker network create`, `ls`, `inspect`, `connect`, `disconnect`, `rm`.
- Compose: `docker compose config`, `up -d`, `ps`, `logs -f`, `restart`, `stop`, `down`.
- Cleanup: named container/image cleanup first; explain the blast radius of `prune` and do not make it the default.

Use BBS-oriented names such as `bbs-mysql`, `bbs-redis`, `bbs-network` and `bbs-mysql-data`. Clearly distinguish image, container, volume and network.

**Step 4: Add troubleshooting flow**

Add this order of checks:

```text
Container exists? -> Running? -> Logs healthy? -> Port mapped? -> Host listening?
-> Firewall open? -> Container network connected? -> Volume/permission correct?
```

Include common symptoms: name conflict, port already allocated, permission denied on Docker socket, image pull timeout, container exits immediately and disk full.

**Step 5: Validate required content**

Run:

```powershell
rg -n "只读检查|改变状态|删除资源|需要 root|systemctl|journalctl|firewall-cmd|getenforce|docker logs|docker exec|docker volume|docker network|docker compose" reference/0001-centos7-linux-docker-command-reference.html
```

Expected: all risk labels and command families are present.

**Step 6: Commit**

```powershell
git add reference/0001-centos7-linux-docker-command-reference.html
git commit -m "docs: add CentOS Linux Docker command reference"
```

### Task 3: Create the visual BBS deployment learning roadmap

**Files:**
- Create: `reference/0002-bbs-docker-learning-roadmap.html`

**Step 1: Create the milestone view**

Build a responsive, printable Chinese roadmap with these stages:

1. Host safety and inspection.
2. Linux operating basics.
3. Docker container lifecycle.
4. MySQL 8.4 and Task 3.
5. Redis.
6. BBS Spring Boot application.
7. Elasticsearch and RocketMQ.
8. Docker Compose integration.

Each stage shows prerequisites, practical task, observable completion evidence and the main commands learned. Mark Redis, Elasticsearch, RocketMQ and RAG integration as future work rather than current project completion.

**Step 2: Add navigation and progress semantics**

Link to:

- `0001-centos7-linux-docker-command-reference.html`.
- `../lessons/0002-centos7-first-docker-container.html` as the starting lesson.
- `../lessons/0001-run-task3-with-linux-vm-docker.html` as the Task 3 milestone.

Use explicit labels for `现在开始`, `下一步`, `后续阶段` and `通过标准`; do not hard-code a false completion state.

**Step 3: Verify scope language and links**

Run:

```powershell
rg -n "现在开始|下一步|后续阶段|通过标准|Redis|Elasticsearch|RocketMQ|Compose" reference/0002-bbs-docker-learning-roadmap.html
Test-Path reference/0001-centos7-linux-docker-command-reference.html
Test-Path lessons/0001-run-task3-with-linux-vm-docker.html
```

Expected: roadmap terms are present and both linked existing files resolve.

**Step 4: Commit**

```powershell
git add reference/0002-bbs-docker-learning-roadmap.html
git commit -m "docs: add BBS Docker learning roadmap"
```

### Task 4: Create the first CentOS 7 Docker lesson

**Files:**
- Create: `lessons/0002-centos7-first-docker-container.html`

**Step 1: Teach safe host inspection**

Create a self-contained lesson that asks the learner to run and understand:

```bash
whoami
cat /etc/os-release
uname -r
free -h
df -h
ip addr
docker version
docker info
systemctl status docker --no-pager
```

Explain which commands are read-only, what output matters and how CentOS 7 end-of-life affects the learning environment.

**Step 2: Teach one complete container lifecycle**

Use an explicitly named practice container and port:

```bash
docker pull nginx:1.27-alpine
docker run -d --name bbs-practice-web -p 8088:80 nginx:1.27-alpine
docker ps
curl http://127.0.0.1:8088
docker logs bbs-practice-web
docker exec bbs-practice-web nginx -v
docker inspect bbs-practice-web
docker stop bbs-practice-web
docker start bbs-practice-web
docker rm -f bbs-practice-web
```

Before each state-changing command, explain its effect. Make cleanup target only `bbs-practice-web`. Explain that removing this stateless practice container is different from deleting a database volume.

**Step 3: Add feedback loop**

End the lesson by requesting this evidence bundle:

```bash
docker version --format '{{.Server.Version}}'
docker ps --filter name=bbs-practice-web
curl -I http://127.0.0.1:8088
docker logs --tail 10 bbs-practice-web
```

Because the cleanup step removes the container, instruct the learner to collect the evidence before cleanup. Add a three-question browser quiz about image versus container, `-p 8088:80`, and logs versus shell access, with immediate answer feedback implemented in plain JavaScript.

**Step 4: Link supporting materials**

Link to the command reference, roadmap, official Docker run reference and the existing Task 3 lesson. State that Task 3 is next only after the evidence bundle passes.

**Step 5: Validate lesson content**

Run:

```powershell
rg -n "bbs-practice-web|8088:80|docker logs|docker exec|docker stop|docker start|docker rm|收集.*证据|Task 3" lessons/0002-centos7-first-docker-container.html
```

Expected: the full lifecycle, evidence checkpoint and Task 3 transition are present.

**Step 6: Commit**

```powershell
git add lessons/0002-centos7-first-docker-container.html
git commit -m "docs: add first CentOS Docker lesson"
```

### Task 5: Adapt the existing Task 3 lesson to CentOS 7

**Files:**
- Modify: `lessons/0001-run-task3-with-linux-vm-docker.html`

**Step 1: Replace the default host instructions**

Change the main path from Ubuntu/Debian to CentOS 7:

- Use `yum`, `systemctl` and CentOS-compatible package checks.
- Check `java -version`, `mvn -version` and `git --version` before attempting installation.
- Explain that CentOS 7 repositories may be unavailable because of end-of-life; do not silently rewrite repository configuration.
- When package installation fails, ask the learner to provide `yum repolist` and the first repository error before changing mirrors.
- Keep JDK 17 or 21 as the project requirement and do not claim the default CentOS 7 repositories provide it.

**Step 2: Add prerequisite navigation**

At the top, link back to:

- `0002-centos7-first-docker-container.html`.
- `../reference/0001-centos7-linux-docker-command-reference.html`.
- `../reference/0002-bbs-docker-learning-roadmap.html`.

Keep the existing secure recommendation: Maven and Docker run inside the VM, and unauthenticated Docker TCP port 2375 stays closed.

**Step 3: Preserve Task 3 acceptance criteria**

Verify the lesson still requires:

- `mysql:8.4` startup.
- Flyway migration to version `1`.
- Both named tests passing.
- `BUILD SUCCESS`, zero failures and zero errors.

**Step 4: Verify CentOS and security language**

Run:

```powershell
rg -n "CentOS 7|yum|systemctl|java -version|mvn -version|mysql:8.4|version.*1|BUILD SUCCESS|2375" lessons/0001-run-task3-with-linux-vm-docker.html
```

Expected: CentOS-specific prerequisites and all original security and completion requirements are present.

**Step 5: Commit**

```powershell
git add lessons/0001-run-task3-with-linux-vm-docker.html
git commit -m "docs: adapt Task 3 Docker lesson for CentOS 7"
```

### Task 6: Verify the learning artifacts and record progress

**Files:**
- Modify: `docs/superpowers/progress/2026-08-11-bbs-phase-1-progress.md`
- Modify only if defects are found: `reference/*.html`
- Modify only if defects are found: `lessons/*.html`

**Step 1: Check repository and relative links**

Run:

```powershell
git diff --check
Get-ChildItem reference,lessons -Filter *.html | Select-Object FullName,Length
rg -n "href=\"(\.\.?/)[^\"]+\"" reference lessons
```

Manually resolve every local `href` target. Expected: no missing local file and no whitespace errors.

**Step 2: Perform visual browser verification**

Invoke `@browser-harness:browser-harness` for all browser interaction and `@webapp-testing` for local UI verification. Open each HTML artifact and check at minimum:

- Desktop viewport around 1440 x 900.
- Mobile viewport around 390 x 844.
- No horizontal page overflow, clipped code, overlapping text or unreadable tables.
- Working table of contents, local navigation and lesson quiz.
- Print preview does not hide essential command content.

Capture screenshots for the command reference, roadmap and first lesson. Fix any defect before continuing.

**Step 3: Check content safety**

Run:

```powershell
rg -n "rm -rf|0\.0\.0\.0:2375|--privileged|setenforce 0|systemctl stop firewalld" reference lessons
```

Expected: no unsafe shortcut is recommended. If a dangerous string is mentioned as a warning, the surrounding text must explicitly say not to use it.

**Step 4: Update persistent progress**

Add a `Linux and Docker learning track` section to the progress file containing:

- Approved design and plan paths.
- The CentOS 7 learning constraint.
- Created reference and lesson paths.
- Visual verification results.
- Current learner checkpoint: awaiting the first lesson evidence bundle.
- Reminder that Task 3 remains pending until the real MySQL container test reports success.

**Step 5: Run final verification**

Invoke `@verification-before-completion`, then run:

```powershell
git diff --check
git status --short
rg -n "Task 3.*pending|awaiting.*evidence|CentOS 7" docs/superpowers/progress/2026-08-11-bbs-phase-1-progress.md
```

Expected: clean diff check, only intentional files pending, and the progress record does not falsely mark Task 3 complete.

**Step 6: Commit**

```powershell
git add docs/superpowers/progress/2026-08-11-bbs-phase-1-progress.md reference lessons RESOURCES.md NOTES.md
git commit -m "docs: verify Linux Docker learning materials"
```

## Completion checkpoint

Implementation is complete only when all artifacts render correctly, local links work, the quiz responds correctly, unsafe shortcuts are absent, and the progress file records that the learner must still complete the first evidence bundle and real Task 3 test.
