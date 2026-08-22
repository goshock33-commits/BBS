package com.bbs.bbs.handller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailTemplateCommentContractTest {

    @Test
    void replyButtonAlwaysSuppliesTheRenderedPostId() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/templates/jie/detail.html")) {
            assertNotNull(input, "detail template must exist");
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(html.contains("onclick=\"return reply()\""));
            assertTrue(html.contains("if (!postId) {\n                postId = $(\"#postId\").val();"));
        }
    }
}
