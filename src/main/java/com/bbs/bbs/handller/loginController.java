package com.bbs.bbs.handller;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.auth.LoginRequest;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.response.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class loginController {
    private final BBSUserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public loginController(BBSUserService userService,
                           AuthenticationManager authenticationManager,
                           SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    @PostMapping("/api/v1/auth/login")
    @ResponseBody
    public ApiResult<Void> login(@Valid @RequestBody LoginRequest request,
                                 HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse) {
        jakarta.servlet.http.HttpSession session = servletRequest.getSession();
        verifyCaptcha(session, request.verifyCode());

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.loginName(), request.password()));
        BBSUser user = userService.findActiveByLoginName(request.loginName());
        userService.recordSuccessfulLogin(user.getUserId());
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(
                context, servletRequest, servletResponse);

        session.setAttribute(Constants.USER_SESSION_KEY, user);
        session.removeAttribute(Constants.VERIFY_CODE_KEY);
        return ApiResult.success(null);
    }

    private void verifyCaptcha(jakarta.servlet.http.HttpSession session,
                               String submittedCode) {
        Object stored = session.getAttribute(Constants.VERIFY_CODE_KEY);
        if (!(stored instanceof String expected)
                || !expected.equalsIgnoreCase(submittedCode)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "验证码错误");
        }
    }
}
