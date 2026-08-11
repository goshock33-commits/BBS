package com.bbs.bbs.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 30) String nickName,
        @NotBlank @Pattern(regexp = "男|女|未知") String gender,
        @NotBlank @Size(max = 50) String location,
        @Size(max = 200) String introduce) {
}
