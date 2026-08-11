package com.bbs.util;

import java.security.MessageDigest;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 18:56:34
 * @Version 1.0
 */
public class SHA256Util {
    private static String byteArrayToHexString(byte[] b) {
        StringBuilder resultSb = new StringBuilder();
        for (byte value : b) {
            resultSb.append(byteToHexString(value));
        }
        return resultSb.toString();
    }
    // 将字节数组转换为十六进制字符串
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n += 256;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    /**
     * SHA256 编码
     *
     * @param origin      原始字符串
     * @param charsetname 字符集（如 "UTF-8"）
     * @return SHA256 编码后的 32 位十六进制字符串
     */
    public static String SHA256Encode(String origin, String charsetname) {
        String resultString = null;
        try {
            resultString = origin;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (charsetname == null || "".equals(charsetname)) {
                resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
            } else {
                resultString = byteArrayToHexString(
                        md.digest(resultString.getBytes(charsetname)));
            }
        } catch (Exception ignored) {
        }
        return resultString;
    }

    private static final String[] hexDigits = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e", "f"
    };
}
