package com.bbs.bbs.service;

import com.bbs.bbs.web.dto.admin.AdminPostFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminCommentFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminSummaryResponse;
import com.bbs.bbs.web.dto.category.CategoryRequest;
import com.bbs.bbs.web.dto.category.CategoryResponse;
import com.bbs.bbs.web.dto.comment.CommentResponse;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.response.PageResult;

public interface AdminModerationService {
    PageResult<PostResponse> listPosts(AdminPostFilterRequest request);

    void hidePost(Long operatorId, Long postId, String reason);

    AdminSummaryResponse summary();

    PageResult<CommentResponse> listComments(AdminCommentFilterRequest request);

    void restorePost(Long operatorId, Long postId, String reason);

    void deletePost(Long operatorId, Long postId, String reason);

    void hideComment(Long operatorId, Long commentId, String reason);

    void restoreComment(Long operatorId, Long commentId, String reason);

    void deleteComment(Long operatorId, Long commentId, String reason);

    void disableUser(Long operatorId, Long userId, String reason);

    void restoreUser(Long operatorId, Long userId, String reason);

    CategoryResponse createCategory(Long operatorId, CategoryRequest request);

    CategoryResponse updateCategory(Long operatorId, Integer categoryId,
                                    CategoryRequest request);

    void setCategoryEnabled(Long operatorId, Integer categoryId, boolean enabled);
}
