package com.bbs.bbs.service;

import com.bbs.bbs.bean.BBSPostComment;
import com.bbs.bbs.bean.RecentCommentListEntity;
import com.bbs.util.PageResult;
import com.bbs.bbs.web.dto.comment.CommentPageRequest;
import com.bbs.bbs.web.dto.comment.CommentResponse;
import com.bbs.bbs.web.dto.comment.CreateCommentRequest;

import java.util.List;

public interface BBSCommentService {
    CommentResponse createComment(Long currentUserId, Long postId, CreateCommentRequest request);
    void deleteComment(Long currentUserId, Long commentId);
    com.bbs.bbs.web.response.PageResult<CommentResponse> listComments(Long postId, CommentPageRequest request);
    /**
     * 增加一条评论
     */
    Boolean addPostComment(BBSPostComment postComment);

    /**
     * 删除一条评论
     */
    Boolean delPostComment(Long commentId, Long userId);

    /**
     * 帖子详情页-评论分页列表
     */
    PageResult getCommentsByPostId(Long postId, int commentPage);

    /**
     * 个人中心-最近评论列表
     */
    List<RecentCommentListEntity> getRecentCommentListByUserId(Long userId);
}
