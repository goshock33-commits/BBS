package com.bbs.bbs.handller;

import com.bbs.bbs.common.Constants;
import com.bbs.bbs.common.ServiceResultEnum;
import com.bbs.bbs.service.BBSUserServiceImp;
import com.bbs.util.PatternUtil;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-22 21:01:17
 * @Version 1.0
 */
@Controller
public class registerController {

    @Autowired
    private BBSUserServiceImp bbsUserServiceImp;

    @GetMapping(value = {"register"})
    public String registerPage(){
        return "user/reg";
    }
    @PostMapping("/register")
    @ResponseBody
    public Result<String> register(@RequestParam("loginName") String loginName,
                           @RequestParam("password") String password
            , @RequestParam("nickName") String nickName
            , @RequestParam("verifyCode") String verifyCode
    , HttpSession session){
        //参数校验
    if(!StringUtils.hasLength(loginName)){
        return ResultGenerator.genFailResult(ServiceResultEnum.LOGIN_NAME_NULL.getResult());
    }
        if (!PatternUtil.isEmail(loginName)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_NAME_NOT_EMAIL.getResult());
        }
        if (!StringUtils.hasLength(password)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_PASSWORD_NULL.getResult());
        }
        if (!StringUtils.hasLength(verifyCode)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_VERIFY_CODE_NULL.getResult());
        }
        //校验验证码
        String verifyCodeInSession = (String) session.getAttribute(Constants.VERIFY_CODE_KEY);
        if(!StringUtils.hasLength(verifyCodeInSession)){
            return ResultGenerator.genFailResult(ServiceResultEnum.SYSTEM_ERROR.getResult());
        }
        if(!verifyCodeInSession.equals(verifyCode.toLowerCase())) {
            return ResultGenerator.genFailResult(ServiceResultEnum.LOGIN_VERIFY_CODE_ERROR.getResult());
        }
        //调用service注册用户
        String result = bbsUserServiceImp.register(loginName, password, nickName);
        if(result.equals(ServiceResultEnum.REGISTER_SUCCESS.getResult())){
            //注册成功,清除验证码缓存
            session.removeAttribute(Constants.VERIFY_CODE_KEY);
            return ResultGenerator.genSuccessResult(result);
        }
        return ResultGenerator.genFailResult(result);
    }
}
