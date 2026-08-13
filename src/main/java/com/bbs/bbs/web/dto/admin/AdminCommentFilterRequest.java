package com.bbs.bbs.web.dto.admin;

import com.bbs.bbs.web.model.ContentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record AdminCommentFilterRequest(
        @Min(1) Integer page,
        @Min(1) @Max(50) Integer size,
        ContentStatus status,
        Long postId,
        Long authorId,
        Instant from,
        Instant to) {

    public AdminCommentFilterRequest {
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
