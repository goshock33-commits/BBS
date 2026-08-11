package com.bbs.bbs.service;

import com.bbs.bbs.bean.BBSPost;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-05-12 16:03:09
 * @Version 1.0
 */
public interface BBSPostCollectService {
    /**
     * 收藏帖子
     */
    Boolean addCollectRecord(Long userId, Long postId);

    /**
     * 取消收藏帖子
     */
    Boolean deleteCollectRecord(Long userId, Long postId);

    /**
     * 验证用户是否已收藏该帖子
     */
    Boolean validUserCollect(Long userId, Long postId);

    /**
     * 获取用户收藏的帖子列表
     */
    List<BBSPost> getCollectRecordsByUserId(Long userId);
}
