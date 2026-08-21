package com.bbs.bbs.config;

import com.bbs.bbs.security.BbsUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@ContextConfiguration(classes = SecurityConfigTest.TestEndpoints.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class})
@Import({SecurityConfig.class, PasswordEncoderConfig.class,
        SecurityConfigTest.TestEndpoints.class})
class SecurityConfigTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    BbsUserDetailsService userDetailsService;

    @RestController
    static class TestEndpoints {
        @GetMapping("/")
        String home() {
            return "home";
        }

        @PutMapping("/api/v1/me/profile")
        void profile() {
        }

        @PostMapping("/api/v1/posts")
        void createPost() {
        }

        @GetMapping("/admin")
        String admin() {
            return "admin";
        }

        @GetMapping("/myCenter")
        String myCenter() {
            return "center";
        }

        @GetMapping("/detail/{postId}")
        String postDetail() {
            return "detail";
        }

        @GetMapping("/api/v1/posts/{postId}")
        String publicPostDetail() {
            return "post";
        }
    }

    @Test
    void homepageIsPublic() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void profileWriteRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/v1/me/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void everyNonAuthApiWriteRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/posts").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedWriteWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/v1/posts")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSecondaryFormLoginEndpointCanBypassCaptcha() throws Exception {
        mvc.perform(post("/security/form-login")
                        .with(csrf())
                        .param("username", "user@example.com")
                        .param("password", "password123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAccessAdmin() throws Exception {
        mvc.perform(get("/admin").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedPageRedirectsAnonymousUserToLogin() throws Exception {
        mvc.perform(get("/myCenter"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void postDetailRedirectsAnonymousUserToLogin() throws Exception {
        mvc.perform(get("/detail/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedUserCanAccessPostDetail() throws Exception {
        mvc.perform(get("/detail/7")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void publicPostApiRemainsAvailableWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/posts/7"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRequiresPostAndCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
