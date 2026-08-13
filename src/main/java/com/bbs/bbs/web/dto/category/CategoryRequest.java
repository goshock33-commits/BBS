package com.bbs.bbs.web.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "分类名称不能为空") @Size(max = 50, message = "分类名称不能超过 50 个字符") String name,
        Integer rank) {
}
