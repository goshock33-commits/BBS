package com.bbs.bbs.upload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UploadStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesFileInConfiguredDirectoryAndExposesResourceLocation() throws Exception {
        Path uploadDirectory = temporaryDirectory.resolve("uploads");
        UploadStorage storage = new UploadStorage(uploadDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "image-content".getBytes());

        Path storedFile = storage.store(file, "generated-avatar.png");

        assertThat(storedFile).isEqualTo(
                uploadDirectory.resolve("generated-avatar.png").toAbsolutePath());
        assertThat(Files.readString(storedFile)).isEqualTo("image-content");
        assertThat(storage.resourceLocation()).isEqualTo(
                uploadDirectory.toAbsolutePath().toUri().toString());
    }

    @Test
    void createsConfiguredDirectoryBeforeItIsRegisteredAsAResourceLocation() {
        Path uploadDirectory = temporaryDirectory.resolve("not-created-yet");

        UploadStorage storage = new UploadStorage(uploadDirectory.toString());

        assertThat(uploadDirectory).isDirectory();
        assertThat(storage.resourceLocation()).endsWith("/");
    }
}
