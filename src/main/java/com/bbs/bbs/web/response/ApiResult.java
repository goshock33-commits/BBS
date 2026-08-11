package com.bbs.bbs.web.response;

import com.bbs.bbs.web.filter.TraceIds;

public record ApiResult<T>(String code, String message, String traceId, T data) {

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>("SUCCESS", "SUCCESS", TraceIds.current(), data);
    }

    public static ApiResult<Void> failure(String code, String message) {
        return new ApiResult<>(code, message, TraceIds.current(), null);
    }
}
