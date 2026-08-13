package com.bbs.bbs.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationRequest(
        @NotBlank(message = "审核原因不能为空")
        @Size(max = 500, message = "审核原因不能超过 500 个字符") String reason) {
}
