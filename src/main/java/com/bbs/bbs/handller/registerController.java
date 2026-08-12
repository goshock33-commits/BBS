package com.bbs.bbs.handller;

import com.bbs.bbs.common.Constants;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.auth.RegisterRequest;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.response.ApiResult;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class registerController {
    private final BBSUserService userService;

    public registerController(BBSUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "user/reg";
    }

    @PostMapping("/api/v1/auth/register")
    @ResponseBody
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest request,
                                    HttpSession session) {
        verifyCaptcha(session, request.verifyCode());
        userService.register(request);
        session.removeAttribute(Constants.VERIFY_CODE_KEY);
        return ApiResult.success(null);
    }

    private void verifyCaptcha(HttpSession session, String submittedCode) {
        Object stored = session.getAttribute(Constants.VERIFY_CODE_KEY);
        if (!(stored instanceof String expected)
                || !expected.equalsIgnoreCase(submittedCode)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "验证码错误");
        }
    }
}
