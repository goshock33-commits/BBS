# BBS Upload Directory Portability Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use test-driven-development to implement this plan task-by-task.

**Goal:** Make file uploads use a configurable, cross-platform directory instead of the hard-coded Windows path.

**Architecture:** Introduce one upload-storage component that normalizes the configured directory, creates it when needed, writes multipart files, and supplies the matching file-resource URI. Both the upload controller and MVC resource mapping depend on that component so their paths cannot drift.

**Tech Stack:** Java 17, Spring Boot 4, Spring MVC, JUnit 5, AssertJ

---

### Task 1: Add the portable storage boundary

**Files:**
- Create: `src/main/java/com/bbs/bbs/upload/UploadStorage.java`
- Create: `src/test/java/com/bbs/bbs/upload/UploadStorageTest.java`

1. Write a test that stores a mock multipart file in a JUnit temporary directory.
2. Run the focused test and confirm it fails because `UploadStorage` is absent.
3. Implement path normalization, directory creation, safe child resolution, and the resource-location URI.
4. Run the focused test and confirm it passes.

### Task 2: Connect uploads and static serving

**Files:**
- Modify: `src/main/java/com/bbs/bbs/handller/uploadController.java`
- Modify: `src/main/java/com/bbs/bbs/config/MyBBSWebMvcConfigurer.java`
- Modify: `src/main/resources/application-local.properties`

1. Inject `UploadStorage` into the upload controller and MVC configurer.
2. Replace direct use of `Constants.FILE_UPLOAD_DIC` with the shared component.
3. Configure `bbs.upload.directory` from `BBS_UPLOAD_DIR`, defaulting to `./upload`.
4. Run focused upload tests and compilation.

### Task 3: Document and verify

**Files:**
- Modify: `.env.example`
- Modify: `HELP.md`
- Modify: `docs/superpowers/progress/2026-08-11-bbs-phase-1-progress.md`

1. Document `BBS_UPLOAD_DIR` for Windows and Linux.
2. Run the non-Docker regression suite, compilation, and `git diff --check`.
3. Re-run the application on CentOS with `/opt/image/upload`, upload an image, and fetch the returned `/upload/...` URL.
4. Record the verification result and commit only after Linux evidence is returned.
