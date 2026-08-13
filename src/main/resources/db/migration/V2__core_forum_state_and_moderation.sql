ALTER TABLE tb_bbs_post
    ADD COLUMN content_status VARCHAR(16) NULL AFTER post_status;

UPDATE tb_bbs_post
SET content_status = CASE
    WHEN is_deleted = 1 THEN 'DELETED'
    WHEN post_status = 1 THEN 'NORMAL'
    ELSE 'HIDDEN'
END;

UPDATE tb_bbs_post
SET post_views = GREATEST(post_views, 0),
    post_comments = GREATEST(post_comments, 0),
    post_collects = GREATEST(post_collects, 0);

ALTER TABLE tb_bbs_post
    MODIFY COLUMN content_status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    MODIFY COLUMN version INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_post_content_status
        CHECK (content_status IN ('NORMAL', 'HIDDEN', 'DELETED')),
    ADD CONSTRAINT chk_post_counters_non_negative
        CHECK (post_views >= 0 AND post_comments >= 0 AND post_collects >= 0),
    ADD INDEX idx_post_content_status_time (content_status, create_time);

ALTER TABLE tb_bbs_post_comment
    ADD COLUMN content_status VARCHAR(16) NULL AFTER comment_status,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER content_status;

UPDATE tb_bbs_post_comment
SET content_status = CASE
    WHEN is_deleted = 1 THEN 'DELETED'
    WHEN comment_status = 1 THEN 'NORMAL'
    ELSE 'HIDDEN'
END;

ALTER TABLE tb_bbs_post_comment
    MODIFY COLUMN content_status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    ADD CONSTRAINT chk_comment_content_status
        CHECK (content_status IN ('NORMAL', 'HIDDEN', 'DELETED')),
    ADD INDEX idx_comment_status_time (content_status, comment_create_time);

-- V1 already introduced category enablement and collection uniqueness. Keep
-- those structures in place while V2 adds the explicit forum state model.
ALTER TABLE tb_bbs_post_category
    MODIFY COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;

CREATE TABLE moderation_record (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    operator_user_id BIGINT NOT NULL,
    previous_status VARCHAR(16) NOT NULL,
    resulting_status VARCHAR(16) NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_moderation_target_type
        CHECK (target_type IN ('POST', 'COMMENT')),
    CONSTRAINT chk_moderation_action
        CHECK (action IN ('HIDE', 'RESTORE', 'DELETE')),
    CONSTRAINT chk_moderation_previous_status
        CHECK (previous_status IN ('NORMAL', 'HIDDEN', 'DELETED')),
    CONSTRAINT chk_moderation_resulting_status
        CHECK (resulting_status IN ('NORMAL', 'HIDDEN', 'DELETED')),
    INDEX idx_moderation_target_time (target_type, target_id, create_time),
    INDEX idx_moderation_operator_time (operator_user_id, create_time)
);

-- Retained from V1 and verified by the Phase 2 migration test:
-- uk_collect_user_post(user_id, post_id)
