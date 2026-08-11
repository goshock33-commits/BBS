package com.bbs.util;

import java.net.URI;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 18:59:45
 * @Version 1.0
 */
public class SystemUtil {
    /**
     * 系统工具类
     */
        /**
         * 清理字符串中的 HTML 标签和特殊字符，防止 XSS 攻击
         */
        public static String cleanString(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }
            // 去除首尾空格
            value = value.trim();
            // 简单去除 HTML 标签
            value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            return value;
        }

        /**
         * 从 URI 中提取主机地址（协议 + 主机 + 端口）
         * 例如：http://localhost:8080/register → http://localhost:8080
         */
        public static String getHost(URI uri) {
            String scheme = uri.getScheme();     // http
            String host = uri.getHost();         // localhost
            int port = uri.getPort();            // 8080
            if (port == -1) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        }
}
