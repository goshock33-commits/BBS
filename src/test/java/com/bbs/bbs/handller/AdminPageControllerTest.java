package com.bbs.bbs.handller;

import com.bbs.bbs.config.PasswordEncoderConfig;
import com.bbs.bbs.config.SecurityConfig;
import com.bbs.bbs.security.BbsUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AdminPageController.class)
@ContextConfiguration(classes = AdminPageController.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class})
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class AdminPageControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    BbsUserDetailsService userDetailsService;

    @Test
    void anonymousAdminVisitRedirectsToLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void ordinaryUserCannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanOpenEveryManagementPage() throws Exception {
        assertAdminPage("/admin", "admin/index");
        assertAdminPage("/admin/posts", "admin/posts");
        assertAdminPage("/admin/comments", "admin/comments");
        assertAdminPage("/admin/users", "admin/users");
        assertAdminPage("/admin/categories", "admin/categories");
    }

    private void assertAdminPage(String path, String viewName) throws Exception {
        mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }
}
