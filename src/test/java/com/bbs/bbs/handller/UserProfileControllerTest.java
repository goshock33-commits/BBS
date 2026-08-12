package com.bbs.bbs.handller;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.service.BBSCommentService;
import com.bbs.bbs.service.BBSPostCollectService;
import com.bbs.bbs.service.BBSPostService;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.user.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BBSUserController.class)
@ContextConfiguration(classes = BBSUserController.class)
@org.springframework.context.annotation.Import(
        UserProfileControllerTest.AuthenticationPrincipalConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {
    @TestConfiguration
    static class AuthenticationPrincipalConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean BBSUserService userService;
    @MockitoBean BBSPostService postService;
    @MockitoBean BBSCommentService commentService;
    @MockitoBean BBSPostCollectService collectService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void profileUpdateUsesAuthenticatedPrincipalIdentity() throws Exception {
        BBSUser current = new BBSUser();
        current.setUserId(42L);
        BBSUser refreshed = new BBSUser();
        refreshed.setUserId(42L);
        refreshed.setNickName("tester");
        when(userService.findActiveByLoginName("user@example.com"))
                .thenReturn(current, refreshed);
        MockHttpSession session = new MockHttpSession();

        mvc.perform(put("/api/v1/me/profile")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":999,"nickName":"tester","gender":"男",
                                 "location":"北京","introduce":"hello"}
                                """))
                .andExpect(status().isOk());

        verify(userService).updateProfile(42L,
                new UpdateProfileRequest("tester", "男", "北京", "hello"));
        org.junit.jupiter.api.Assertions.assertSame(refreshed,
                session.getAttribute(com.bbs.bbs.common.Constants.USER_SESSION_KEY));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void passwordChangeInvalidatesCurrentSession() throws Exception {
        BBSUser current = new BBSUser();
        current.setUserId(42L);
        when(userService.findActiveByLoginName("user@example.com"))
                .thenReturn(current);
        MockHttpSession session = new MockHttpSession();

        mvc.perform(put("/api/v1/me/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalPassword":"password123",
                                 "newPassword":"newPassword456"}
                                """))
                .andExpect(status().isOk());

        verify(userService).changePassword(42L,
                new com.bbs.bbs.web.dto.user.ChangePasswordRequest(
                        "password123", "newPassword456"));
        assertTrue(session.isInvalid());
    }
}
