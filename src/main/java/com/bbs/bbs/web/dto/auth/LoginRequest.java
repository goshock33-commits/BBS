package com.bbs.bbs.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 100) String loginName,
        @NotBlank @Size(max = 72) String password,
        @NotBlank @Size(min = 4, max = 8) String verifyCode) {
}
