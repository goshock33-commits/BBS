package com.bbs.bbs.web.dto.collect;

import java.time.Instant;

public record CollectResponse(Long postId, boolean collected, Instant collectTime) {
}
