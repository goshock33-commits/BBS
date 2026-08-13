package com.bbs.bbs.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class UploadStorage {
    private final Path directory;

    public UploadStorage(
            @Value("${bbs.upload.directory:./upload}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot create upload directory: " + this.directory,
                    exception);
        }
    }

    public Path store(MultipartFile file, String fileName) throws IOException {
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid upload file name");
        }
        file.transferTo(target);
        return target;
    }

    public String resourceLocation() {
        return directory.toUri().toString();
    }
}
