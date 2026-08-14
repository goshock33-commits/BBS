package com.bbs.bbs.handller.api;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.config.SecurityConfig;
import com.bbs.bbs.security.BbsUserDetailsService;
import com.bbs.bbs.service.AdminModerationService;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.admin.AdminPostFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminCommentFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminSummaryResponse;
import com.bbs.bbs.web.dto.category.CategoryRequest;
import com.bbs.bbs.web.dto.category.CategoryResponse;
import com.bbs.bbs.web.dto.comment.CommentResponse;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.error.GlobalExceptionHandler;
import com.bbs.bbs.web.model.ContentStatus;
import com.bbs.bbs.web.response.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminApiController.class)
@ContextConfiguration(classes = AdminApiController.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminApiControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminModerationService moderationService;
    @MockitoBean
    BBSUserService userService;
    @MockitoBean
    BbsUserDetailsService userDetailsService;

    private BBSUser administrator;

    @BeforeEach
    void setUp() {
        administrator = new BBSUser();
        administrator.setUserId(9L);
        administrator.setLoginName("admin@example.com");
        administrator.setRole("ADMIN");
        administrator.setUserStatus((byte) 0);
    }

    @Test
    void userCannotAccessAdminApi() throws Exception {
        mvc.perform(get("/api/v1/admin/summary")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanFilterPostsWithStablePageEnvelope() throws Exception {
        when(moderationService.listPosts(any(AdminPostFilterRequest.class)))
                .thenReturn(PageResult.of(List.<PostResponse>of(), 0, 2, 20));

        mvc.perform(get("/api/v1/admin/posts")
                        .param("page", "2")
                        .param("size", "20")
                        .param("status", "HIDDEN")
                        .param("categoryId", "3")
                        .param("authorId", "7")
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-14T00:00:00Z")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(moderationService).listPosts(new AdminPostFilterRequest(
                2, 20, ContentStatus.HIDDEN, 3, 7L,
                java.time.Instant.parse("2026-08-01T00:00:00Z"),
                java.time.Instant.parse("2026-08-14T00:00:00Z")));
    }

    @Test
    void hideRejectsBlankReasonBeforeCallingService() throws Exception {
        mvc.perform(post("/api/v1/admin/posts/41/hide")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(moderationService, never())
                .hidePost(any(), any(), any());
    }

    @Test
    void hideUsesAuthenticatedAdministratorIdentity() throws Exception {
        when(userService.findActiveByLoginName("admin@example.com"))
                .thenReturn(administrator);

        mvc.perform(post("/api/v1/admin/posts/41/hide")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"违反版规\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(moderationService).hidePost(9L, 41L, "违反版规");
    }

    @Test
    void adminCanReadSummaryAndFilterComments() throws Exception {
        when(moderationService.summary())
                .thenReturn(new AdminSummaryResponse(11, 22, 33));
        when(moderationService.listComments(any(AdminCommentFilterRequest.class)))
                .thenReturn(PageResult.of(List.<CommentResponse>of(), 0, 1, 10));

        mvc.perform(get("/api/v1/admin/summary")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users").value(11))
                .andExpect(jsonPath("$.data.posts").value(22))
                .andExpect(jsonPath("$.data.comments").value(33));

        mvc.perform(get("/api/v1/admin/comments")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "DELETED")
                        .param("postId", "41")
                        .param("authorId", "7")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1));

        verify(moderationService).listComments(new AdminCommentFilterRequest(
                1, 10, ContentStatus.DELETED, 41L, 7L, null, null));
    }

    @Test
    void contentAndUserActionsUseAdministratorIdentity() throws Exception {
        when(userService.findActiveByLoginName("admin@example.com"))
                .thenReturn(administrator);

        performReasonAction("/api/v1/admin/posts/41/restore", "复核通过");
        performReasonAction("/api/v1/admin/posts/41/delete", "严重违规");
        performReasonAction("/api/v1/admin/comments/51/hide", "不当言论");
        performReasonAction("/api/v1/admin/comments/51/restore", "复核通过");
        performReasonAction("/api/v1/admin/comments/51/delete", "严重违规");
        performReasonAction("/api/v1/admin/users/7/disable", "封禁账号");
        performReasonAction("/api/v1/admin/users/7/restore", "申诉通过");

        verify(moderationService).restorePost(9L, 41L, "复核通过");
        verify(moderationService).deletePost(9L, 41L, "严重违规");
        verify(moderationService).hideComment(9L, 51L, "不当言论");
        verify(moderationService).restoreComment(9L, 51L, "复核通过");
        verify(moderationService).deleteComment(9L, 51L, "严重违规");
        verify(moderationService).disableUser(9L, 7L, "封禁账号");
        verify(moderationService).restoreUser(9L, 7L, "申诉通过");
    }

    @Test
    void categoryRoutesUseDtoAndAdministratorIdentity() throws Exception {
        when(userService.findActiveByLoginName("admin@example.com"))
                .thenReturn(administrator);
        when(moderationService.listCategories()).thenReturn(List.of(
                new CategoryResponse(3, "后端", 8, true),
                new CategoryResponse(4, "归档", 1, false)));
        when(moderationService.createCategory(9L,
                new CategoryRequest("后端", 8)))
                .thenReturn(new CategoryResponse(3, "后端", 8, true));
        when(moderationService.updateCategory(9L, 3,
                new CategoryRequest("Java", 9)))
                .thenReturn(new CategoryResponse(3, "Java", 9, true));

        mvc.perform(get("/api/v1/admin/categories")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].enabled").value(false));
        mvc.perform(post("/api/v1/admin/categories")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"后端\",\"rank\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("后端"));
        mvc.perform(put("/api/v1/admin/categories/3")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\",\"rank\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Java"));
        mvc.perform(post("/api/v1/admin/categories/3/enable")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/categories/3/disable")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(moderationService).setCategoryEnabled(9L, 3, true);
        verify(moderationService).setCategoryEnabled(9L, 3, false);
    }

    private void performReasonAction(String path, String reason) throws Exception {
        mvc.perform(post(path)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }
}
