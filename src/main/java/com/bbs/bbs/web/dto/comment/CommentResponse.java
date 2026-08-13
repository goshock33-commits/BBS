package com.bbs.bbs.web.dto.comment;

import com.bbs.bbs.web.model.ContentStatus;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        Long parentCommentId,
        String body,
        ContentStatus status,
        Long version,
        Instant createTime) {
}
