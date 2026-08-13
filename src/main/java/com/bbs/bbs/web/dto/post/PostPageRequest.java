package com.bbs.bbs.web.dto.post;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PostPageRequest(
        @Min(value = 1, message = "页码必须从 1 开始") Integer page,
        @Min(value = 1, message = "每页至少 1 条") @Max(value = 50, message = "每页最多 50 条") Integer size,
        Integer categoryId,
        String keyword,
        String period,
        String orderBy) {

    public PostPageRequest {
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
