package com.bbs.bbs.handller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageTemplateContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");

    @Test
    void adminPagesUseCanonicalModerationApisAndSharedCsrfClient() throws IOException {
        String index = template("admin/index.html");
        String posts = template("admin/posts.html");
        String comments = template("admin/comments.html");
        String users = template("admin/users.html");
        String categories = template("admin/categories.html");
        String client = resource("static/js/admin.js");

        assertThat(index).contains("/api/v1/admin/summary");
        assertThat(posts).contains("/api/v1/admin/posts", "'hide'", "'restore'", "'delete'");
        assertThat(posts).contains("post.content", "查看正文");
        assertThat(comments).contains("/api/v1/admin/comments", "'hide'", "'restore'", "'delete'");
        assertThat(users).contains("/api/v1/admin/users/",
                "data-action=\"disable\"", "data-action=\"restore\"");
        assertThat(categories).contains("/api/v1/admin/categories",
                "data-action=\"enable\"", "data-action=\"disable\"",
                "categoryRows", "loadCategories");
        assertThat(client).contains("meta[name='_csrf']", "meta[name='_csrf_header']");
        assertThat(List.of(index, posts, comments, users, categories))
                .allSatisfy(page -> assertThat(page).contains("/js/admin.js"));
    }

    @Test
    void legacyPagesWriteOnlyThroughCanonicalForumApis() throws IOException {
        String add = template("jie/add.html");
        String edit = template("jie/edit.html");
        String detail = template("jie/detail.html");
        String center = template("user/index.html");

        assertThat(add).contains("/api/v1/posts").doesNotContain("url: '/addPost'");
        assertThat(edit).contains("/api/v1/posts/").doesNotContain("url = '/editPost'");
        assertThat(detail)
                .contains("/api/v1/posts/", "/comments", "/collect")
                .doesNotContain("url = '/replyPost'", "url: '/delReply/",
                        "url: '/addCollect/", "url: '/delCollect/");
        assertThat(center).contains("/api/v1/posts/")
                .doesNotContain("url: '/delPost/");
        assertThat(List.of(add, edit, detail, center))
                .allSatisfy(page -> assertThat(page)
                        .contains("meta[name='_csrf']", "meta[name='_csrf_header']"));
    }

    @Test
    void sharedPagesExposeModerationStateWithoutOfferingInvalidActions() throws IOException {
        String center = template("user/index.html");
        String header = template("header.html");

        assertThat(center)
                .contains("post-status-hidden", "已隐藏")
                .contains("bbsPost.contentStatus.name() == 'NORMAL'");
        assertThat(header)
                .contains("session.myBBSUser.role == 'ADMIN'", "@{/admin}", "管理后台");
    }

    private String template(String relative) throws IOException {
        return resource("templates/" + relative);
    }

    private String resource(String relative) throws IOException {
        Path path = RESOURCES.resolve(relative);
        return Files.exists(path)
                ? Files.readString(path, StandardCharsets.UTF_8)
                : "";
    }
}
