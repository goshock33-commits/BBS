package com.bbs.bbs.handller;

import com.bbs.bbs.common.Constants;
import com.bbs.bbs.common.ServiceResultEnum;
import com.bbs.bbs.service.BBSUserServiceImp;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import com.bbs.util.SHA256Util;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-23 20:30:55
 * @Version 1.0
 */
@Controller
public class loginController {
    @Resource
    private BBSUserServiceImp bbsUserServiceImp;
    @GetMapping(value = {"login"})
    public String loginPage(){
        return "user/login";
    }
    @PostMapping("/login")
    @ResponseBody
    public Result<String> login(@RequestParam("loginName") String loginName,
                        @RequestParam("password") String password
            , @RequestParam("verifyCode") String verifyCode
            , HttpSession httpSession){
        //参数校验
        if (!StringUtils.hasLength(loginName)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_NAME_NULL.getResult());
        }
        if (!StringUtils.hasLength(password)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_PASSWORD_NULL.getResult());
        }
        //校验验证码TOdo
        if (!StringUtils.hasLength(verifyCode)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_VERIFY_CODE_NULL.getResult());
        }
        //校验验证码
        String verifyCodeInSession = (String) httpSession.getAttribute(Constants.VERIFY_CODE_KEY);
        if(!StringUtils.hasLength(verifyCodeInSession)){
            return ResultGenerator.genFailResult(ServiceResultEnum.SYSTEM_ERROR.getResult());
        }
        if(!verifyCodeInSession.equals(verifyCode.toLowerCase())) {
            return ResultGenerator.genFailResult(ServiceResultEnum.LOGIN_VERIFY_CODE_ERROR.getResult());
        }
        //加密 密码
        String passwordMD5 = SHA256Util.SHA256Encode(password,"UTF-8");
        String result = bbsUserServiceImp.login(loginName, passwordMD5, httpSession);
        if(ServiceResultEnum.LOGIN_SUCCESS.getResult().equals(result)){
            return ResultGenerator.genSuccessResult(result);
        }
        return ResultGenerator.genFailResult(result);
    }
    @GetMapping("/logout")
    public String logOut(HttpSession httpSession){
       httpSession.removeAttribute(Constants.USER_SESSION_KEY);
        return "redirect:/login";
    }
}
