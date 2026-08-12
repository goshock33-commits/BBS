package com.bbs.bbs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {
    private static final RequestMatcher API_REQUEST =
            PathPatternRequestMatcher.pathPattern("/api/**");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint apiEntryPoint =
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        AuthenticationEntryPoint pageEntryPoint =
                (request, response, exception) -> response.sendRedirect("/login");
        AccessDeniedHandler accessDeniedHandler = (request, response, exception) ->
                response.sendError(HttpStatus.FORBIDDEN.value());

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/detail/**", "/search",
                                "/userCenter/**", "/login", "/register",
                                "/common/captcha", "/api/v1/auth/login",
                                "/api/v1/auth/register", "/css/**", "/js/**",
                                "/images/**", "/layui/**", "/upload/**").permitAll()
                        .requestMatchers("/admin", "/admin/**", "/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/me/**", "/api/v1/**",
                                "/addPostPage", "/addPost", "/editPostPage/**",
                                "/editPost", "/delPost/**", "/replyPost",
                                "/delReply/**", "/addCollect/**", "/deleteCollect/**",
                                "/uploadFile", "/uploadFiles", "/updateUserInfo",
                                "/updateHeadImg", "/updatePassword", "/myCenter",
                                "/userSet").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(apiEntryPoint, API_REQUEST)
                        .defaultAuthenticationEntryPointFor(pageEntryPoint,
                                request -> !API_REQUEST.matches(request))
                        .accessDeniedHandler(accessDeniedHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"));
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
