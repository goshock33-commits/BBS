package com.bbs.bbs.web.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReplyRequest(
        @NotNull(message = "父评论不能为空") Long parentCommentId,
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 200, message = "回复不能超过 200 个字符") String body) {
}
