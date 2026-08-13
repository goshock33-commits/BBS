package com.bbs.bbs.web.dto.post;

import com.bbs.bbs.web.model.ContentStatus;

import java.time.Instant;

public record PostResponse(
        Long id,
        Long authorId,
        String authorName,
        Integer categoryId,
        String categoryName,
        String title,
        String content,
        ContentStatus status,
        Long views,
        Long comments,
        Long collects,
        Long version,
        Instant createTime,
        Instant updateTime) {
}
