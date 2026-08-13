package com.bbs.bbs.common;

import java.util.Set;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 18:35:03
 * @Version 1.0
 */
public class Constants {
    // Session 中存储用户对象的 Key
    public final static String USER_SESSION_KEY = "myBBSUser";

    // Session 中存储验证码的 Key
    public final static String VERIFY_CODE_KEY = "userVerifyCode";
    // 允许上传的文件类型
    public final static Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );
    public final static String HOt_POST="hotPosts";
}
