package com.bbs.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 18:57:19
 * @Version 1.0
 */
public class PatternUtil {
    /**
     * 匹配邮箱格式
     */
    public static boolean isEmail(String email) {
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@"
                + "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
