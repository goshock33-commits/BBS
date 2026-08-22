package com.bbs.bbs.handller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyPostTemplateContractTest {

    @Test
    void wangEditorUploadIncludesCsrfHeadersOnCreateAndEditPages() throws IOException {
        String add = readTemplate("jie/add.html");
        String edit = readTemplate("jie/edit.html");

        assertTrue(add.contains("uploadImgHeaders = csrfHeaders()"));
        assertTrue(edit.contains("uploadImgHeaders = csrfHeaders()"));
    }

    @Test
    void categoryLinksUseTheCategoryIdAcceptedByIndexController() throws IOException {
        String index = readTemplate("index.html");

        assertTrue(index.contains("categoryId=${c.categoryId}"));
        assertFalse(index.contains("'/index?categoryName='+c.categoryName"));
    }

    private String readTemplate(String name) throws IOException {
        return Files.readString(Path.of("src/main/resources/templates", name),
                StandardCharsets.UTF_8);
    }
}
