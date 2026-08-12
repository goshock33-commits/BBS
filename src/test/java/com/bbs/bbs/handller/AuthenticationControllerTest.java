package com.bbs.bbs.handller;

import com.bbs.bbs.common.Constants;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.error.GlobalExceptionHandler;
import com.bbs.bbs.web.filter.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {registerController.class, loginController.class})
@ContextConfiguration(classes = {registerController.class, loginController.class})
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    BBSUserService userService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @Test
    void invalidEmailReturnsValidationError() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .sessionAttr(Constants.VERIFY_CODE_KEY, "abcd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginName":"bad","password":"password123",
                                 "nickName":"tester","verifyCode":"abcd"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        doThrow(new BusinessException(
                ErrorCode.DUPLICATE_LOGIN_NAME, "该邮箱已注册"))
                .when(userService).register(any());

        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .sessionAttr(Constants.VERIFY_CODE_KEY, "abcd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginName":"user@example.com","password":"password123",
                                 "nickName":"tester","verifyCode":"abcd"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_NAME"));
    }

    @Test
    void invalidCaptchaDoesNotRegisterUser() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .sessionAttr(Constants.VERIFY_CODE_KEY, "abcd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginName":"user@example.com","password":"password123",
                                 "nickName":"tester","verifyCode":"wrong"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(userService, never()).register(any());
    }

    @Test
    void successfulLoginPersistsSecurityContextAndCompatibilitySession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String originalSessionId = session.getId();
        session.setAttribute(Constants.VERIFY_CODE_KEY, "abcd");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        BBSUser user = new BBSUser();
        user.setUserId(42L);
        user.setLoginName("user@example.com");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userService.findActiveByLoginName("user@example.com")).thenReturn(user);

        mvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginName":"user@example.com","password":"password123",
                                 "verifyCode":"ABCD"}
                                """))
                .andExpect(status().isOk());

        verify(securityContextRepository).saveContext(any(), any(), any());
        verify(userService).recordSuccessfulLogin(42L);
        assertSame(user, session.getAttribute(Constants.USER_SESSION_KEY));
        assertNull(session.getAttribute(Constants.VERIFY_CODE_KEY));
        assertNotEquals(originalSessionId, session.getId());
    }

    @Test
    void getLogoutEndpointDoesNotExist() throws Exception {
        mvc.perform(get("/logout"))
                .andExpect(status().isNotFound());
    }

    @Test
    void failedCompatibilityUserLookupDoesNotPersistAuthenticatedContext() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userService.findActiveByLoginName("user@example.com"))
                .thenThrow(new BusinessException(
                        ErrorCode.ACCOUNT_DISABLED, "账号已被禁用"));

        mvc.perform(post("/api/v1/auth/login")
                        .sessionAttr(Constants.VERIFY_CODE_KEY, "abcd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginName":"user@example.com","password":"password123",
                                 "verifyCode":"abcd"}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(securityContextRepository);
    }
}
