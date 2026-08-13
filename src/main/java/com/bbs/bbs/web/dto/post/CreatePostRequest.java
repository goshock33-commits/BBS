package com.bbs.bbs.web.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull(message = "分类不能为空") @Positive(message = "分类不合法") Integer categoryId,
        @NotBlank(message = "标题不能为空") @Size(max = 32, message = "标题不能超过 32 个字符") String title,
        @NotBlank(message = "正文不能为空") @Size(max = 100_000, message = "正文不能超过 100000 个字符") String content,
        @Size(max = 5, message = "图片不能超过 5 张") java.util.List<String> imageUrls) {
}
