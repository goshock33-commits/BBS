package com.bbs.bbs.service;

import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.HotPost;
import com.bbs.util.PageQueryUtil;
import com.bbs.util.PageResult;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-27 14:34:46
 * @Version 1.0
 */
public interface BBSPostService {
    // 发帖
    int  addPost(BBSPost post, BBSUser user);
    // 根据分类名查询帖子
    List<BBSPost> selectPostByCategoryName(String categoryName, Integer pageNum);
    long  getTotalPostCount(String categoryName);
    /**
     * 查询帖子详情并将浏览量 +1
     */
    BBSPost getBBSPostById( Long postId);
    BBSPost getBBSPostForDetail(Long postId, Long userId);

    /**
     * 更新帖子
     */
    int updateBBSPost(BBSPost bbsPost);

    /**
     * 删除帖子（软删除，仅本人可操作）
     */
    int delBBSPost(Long userId, Long postId);

    /**
     * 获取本周热议帖子列表（按评论量排序，取前5条）
     */
    List<HotPost> getHotTopicBBSPostList();
    /**
     * 根据userId获取最近发帖列表（用户主页用）
     */
    List<BBSPost> getRecentPostListByUserId(Long userId);

    /**
     * 根据userId查询发布的所有帖子（我的中心用）
     */
    List<BBSPost> getMyBBSPostList(Long userId);

    PageResult getBBSPostPageForIndex(PageQueryUtil pageUtil);
}
