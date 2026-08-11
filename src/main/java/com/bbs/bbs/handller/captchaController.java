package com.bbs.bbs.handller;

import com.bbs.bbs.common.Constants;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-24 11:20:26
 * @Version 1.0
 */
@Controller
public class captchaController {
    @GetMapping("/common/captcha")
    public void defaultCaptcha(HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        // 设置响应头，禁止缓存
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/gif");

        // 创建 静态 验证码：宽75px，高30px，5位字符
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);

        // 设置字符类型：数字和字母混合
        captcha.setCharType(Captcha.TYPE_DEFAULT);

        // 设置字体
        captcha.setFont(Captcha.FONT_9);

        // 将验证码文本存入 Session（转小写，方便后续比较时忽略大小写）
        request.getSession().setAttribute(
                Constants.VERIFY_CODE_KEY,
                captcha.text().toLowerCase());

        // 输出验证码图片流
        captcha.out(response.getOutputStream());
    }
}
