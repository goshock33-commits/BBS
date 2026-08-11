CREATE TABLE tb_bbs_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    login_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nick_name VARCHAR(30) NOT NULL,
    head_img_url VARCHAR(255) NOT NULL DEFAULT '/images/avatar/default.png',
    gender VARCHAR(10) NOT NULL DEFAULT '未知',
    location VARCHAR(50) NOT NULL DEFAULT '未知',
    introduce VARCHAR(200) NOT NULL DEFAULT '',
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    user_status TINYINT NOT NULL DEFAULT 0,
    last_login_time DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_bbs_user_login_name UNIQUE (login_name)
);

CREATE TABLE tb_bbs_post_category (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL,
    category_rank INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_bbs_category_name UNIQUE (category_name)
);

CREATE TABLE tb_bbs_post (
    post_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    publish_user_id BIGINT NOT NULL,
    post_title VARCHAR(32) NOT NULL,
    post_content MEDIUMTEXT NOT NULL,
    post_category_id INT NOT NULL,
    post_category_name VARCHAR(50) NOT NULL,
    post_status TINYINT NOT NULL DEFAULT 1,
    post_views BIGINT NOT NULL DEFAULT 0,
    post_comments BIGINT NOT NULL DEFAULT 0,
    post_collects BIGINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    last_update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_post_category_time (post_category_id, create_time),
    INDEX idx_post_status_time (post_status, is_deleted, create_time)
);

CREATE TABLE tb_bbs_post_comment (
    comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    comment_user_id BIGINT NOT NULL,
    comment_body VARCHAR(200) NOT NULL,
    parent_comment_user_id BIGINT NULL,
    comment_status TINYINT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    comment_create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_comment_post_time (post_id, comment_create_time)
);

CREATE TABLE tb_bbs_post_collect (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_collect_user_post UNIQUE (user_id, post_id)
);

INSERT INTO tb_bbs_post_category(category_name, category_rank)
VALUES ('提问', 10), ('分享', 20), ('讨论', 30);
