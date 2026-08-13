package com.bbs.bbs.web.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 200, message = "评论不能超过 200 个字符") String body,
        Long parentCommentId) {
}
