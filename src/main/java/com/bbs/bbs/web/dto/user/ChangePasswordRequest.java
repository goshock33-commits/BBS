package com.bbs.bbs.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 72) String originalPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
