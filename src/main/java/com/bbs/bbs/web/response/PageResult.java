package com.bbs.bbs.web.response;

import java.util.List;

public record PageResult<T>(
        List<T> list,
        long total,
        int page,
        int size,
        int totalPages) {

    public PageResult {
        list = List.copyOf(list);
    }

    public PageResult(List<T> list, long total, int page, int size) {
        this(list, total, page, size,
                total == 0 ? 0 : (int) Math.ceil((double) total / size));
    }

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(list, total, page, size, totalPages);
    }
}
