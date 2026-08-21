CREATE TABLE tb_bbs_post_view (
    view_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (view_id),
    UNIQUE KEY uk_post_view_post_user (post_id, user_id),
    KEY idx_post_view_user (user_id),
    CONSTRAINT fk_post_view_post FOREIGN KEY (post_id)
        REFERENCES tb_bbs_post (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_view_user FOREIGN KEY (user_id)
        REFERENCES tb_bbs_user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
