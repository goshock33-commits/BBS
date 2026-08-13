package com.bbs.bbs.handller.api;

import com.bbs.bbs.bean.BBSPostCategory;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.config.SecurityConfig;
import com.bbs.bbs.handller.api.CategoryApiController;
import com.bbs.bbs.handller.api.CollectApiController;
import com.bbs.bbs.handller.api.CommentApiController;
import com.bbs.bbs.handller.api.PostApiController;
import com.bbs.bbs.service.*;
import com.bbs.bbs.web.dto.collect.CollectResponse;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.error.GlobalExceptionHandler;
import com.bbs.bbs.web.filter.TraceIdFilter;
import com.bbs.bbs.web.model.ContentStatus;
import com.bbs.bbs.web.response.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PostApiController.class, CommentApiController.class,
        CollectApiController.class, CategoryApiController.class})
@ContextConfiguration(classes = {PostApiController.class, CommentApiController.class,
        CollectApiController.class, CategoryApiController.class})
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, TraceIdFilter.class})
@AutoConfigureMockMvc
class CoreForumControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean BBSPostService postService;
    @MockitoBean BBSCommentService commentService;
    @MockitoBean BBSPostCollectService collectService;
    @MockitoBean BBSPostCategoryService categoryService;
    @MockitoBean BBSUserService userService;
    @MockitoBean UserDetailsService userDetailsService;

    private BBSUser currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new BBSUser();
        currentUser.setUserId(7L);
        currentUser.setLoginName("user@example.com");
        currentUser.setUserStatus((byte) 0);
    }

    @Test
    void anonymousCanReadPostsAndStablePageEnvelope() throws Exception {
        when(postService.listPublicPosts(any())).thenReturn(PageResult.of(List.of(), 0, 1, 10));
        mvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void anonymousCanReadPostDetailCommentsAndCategories() throws Exception {
        when(postService.getVisiblePost(10L, null, false)).thenReturn(postResponse());
        when(commentService.listComments(eq(10L), any())).thenReturn(PageResult.of(List.of(), 0, 1, 10));
        BBSPostCategory category = new BBSPostCategory();
        category.setCategoryId(1); category.setCategoryName("Java"); category.setCategoryRank(10); category.setEnabled(true);
        when(categoryService.getBBSPostCategories()).thenReturn(List.of(category));

        mvc.perform(get("/api/v1/posts/10")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/posts/10/comments")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].enabled").value(true));
    }

    @Test
    void anonymousWriteIsUnauthorizedAndAuthenticatedWriteNeedsCsrf() throws Exception {
        mvc.perform(post("/api/v1/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hello\",\"content\":\"body\",\"categoryId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/posts").with(user("user@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hello\",\"content\":\"body\",\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUsesPrincipalIdentityAndRejectsInvalidDto() throws Exception {
        when(userService.findActiveByLoginName("user@example.com")).thenReturn(currentUser);
        when(postService.createPost(eq(7L), any())).thenReturn(postResponse());
        mvc.perform(post("/api/v1/posts").with(user("user@example.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hello\",\"content\":\"body\",\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorId").value(7));
        verify(postService).createPost(eq(7L), any());

        mvc.perform(post("/api/v1/posts").with(user("user@example.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"\",\"categoryId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void commentDeleteAndCollectRoutesDelegateWithPrincipalIdentity() throws Exception {
        when(userService.findActiveByLoginName("user@example.com")).thenReturn(currentUser);
        when(collectService.collect(7L, 10L)).thenReturn(new CollectResponse(10L, true, Instant.now()));
        when(collectService.cancelCollect(7L, 10L)).thenReturn(new CollectResponse(10L, false, null));

        mvc.perform(delete("/api/v1/comments/5").with(user("user@example.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/posts/10/collect").with(user("user@example.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.collected").value(true));
        mvc.perform(delete("/api/v1/posts/10/collect").with(user("user@example.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.collected").value(false));
        verify(commentService).deleteComment(7L, 5L);
    }

    private PostResponse postResponse() {
        return new PostResponse(10L, 7L, "user", 1, "Java", "hello", "body",
                ContentStatus.NORMAL, 0L, 0L, 0L, 0L, Instant.now(), Instant.now());
    }
}
