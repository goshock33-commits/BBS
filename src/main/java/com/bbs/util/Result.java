package com.bbs.util;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 18:43:50
 * @Version 1.0
 */
@Data
@ToString
@NoArgsConstructor
//统一响应Result
public class Result <T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int resultCode;
    private String message;
    private T data;
    public Result(int resultCode, String message) {
        this.resultCode = resultCode;
        this.message = message;
    }

}
