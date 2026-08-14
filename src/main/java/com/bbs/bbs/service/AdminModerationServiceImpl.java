package com.bbs.bbs.service;

import com.bbs.bbs.Mapper.BBSCategoryMapper;
import com.bbs.bbs.Mapper.BBSCommentMapper;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.Mapper.ModerationRecordMapper;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSPostCategory;
import com.bbs.bbs.bean.BBSPostComment;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.ModerationRecord;
import com.bbs.bbs.web.dto.admin.AdminCommentFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminPostFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminSummaryResponse;
import com.bbs.bbs.web.dto.category.CategoryRequest;
import com.bbs.bbs.web.dto.category.CategoryResponse;
import com.bbs.bbs.web.dto.comment.CommentResponse;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.model.ContentStatus;
import com.bbs.bbs.web.response.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class AdminModerationServiceImpl implements AdminModerationService {
    private final BBSPostMapper postMapper;
    private final BBSCommentMapper commentMapper;
    private final BBSUserMapper userMapper;
    private final BBSCategoryMapper categoryMapper;
    private final ModerationRecordMapper moderationRecordMapper;

    public AdminModerationServiceImpl(BBSPostMapper postMapper,
                                      BBSCommentMapper commentMapper,
                                      BBSUserMapper userMapper,
                                      BBSCategoryMapper categoryMapper,
                                      ModerationRecordMapper moderationRecordMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.moderationRecordMapper = moderationRecordMapper;
    }

    @Override
    public PageResult<PostResponse> listPosts(AdminPostFilterRequest request) {
        AdminPostFilterRequest normalized = normalize(request);
        int offset = (normalized.page() - 1) * normalized.size();
        List<PostResponse> list = postMapper.selectAdminPage(
                        normalized, offset, normalized.size()).stream()
                .map(this::toPostResponse)
                .toList();
        return PageResult.of(list, postMapper.countAdminPage(normalized),
                normalized.page(), normalized.size());
    }

    @Override
    @Transactional
    public void hidePost(Long operatorId, Long postId, String reason) {
        moderatePost(operatorId, postId, reason, ContentStatus.NORMAL,
                ContentStatus.HIDDEN, (byte) 0, "HIDE");
    }

    @Override
    public AdminSummaryResponse summary() {
        return new AdminSummaryResponse(userMapper.countAll(), postMapper.countAll(),
                commentMapper.countAll());
    }

    @Override
    public PageResult<CommentResponse> listComments(AdminCommentFilterRequest request) {
        AdminCommentFilterRequest normalized = normalize(request);
        int offset = (normalized.page() - 1) * normalized.size();
        List<CommentResponse> list = commentMapper.selectAdminPage(
                        normalized, offset, normalized.size()).stream()
                .map(this::toCommentResponse)
                .toList();
        return PageResult.of(list, commentMapper.countAdminPage(normalized),
                normalized.page(), normalized.size());
    }

    @Override
    @Transactional
    public void restorePost(Long operatorId, Long postId, String reason) {
        moderatePost(operatorId, postId, reason, ContentStatus.HIDDEN,
                ContentStatus.NORMAL, (byte) 0, "RESTORE");
    }

    @Override
    @Transactional
    public void deletePost(Long operatorId, Long postId, String reason) {
        BBSPost post = requirePost(postId);
        if (post.getContentStatus() != ContentStatus.HIDDEN) {
            throw invalidTransition("帖子必须先隐藏才能删除");
        }
        moderatePost(operatorId, postId, reason, ContentStatus.HIDDEN,
                ContentStatus.DELETED, (byte) 1, "DELETE");
    }

    @Override
    @Transactional
    public void hideComment(Long operatorId, Long commentId, String reason) {
        moderateComment(operatorId, commentId, reason, ContentStatus.NORMAL,
                ContentStatus.HIDDEN, (byte) 0, "HIDE");
    }

    @Override
    @Transactional
    public void restoreComment(Long operatorId, Long commentId, String reason) {
        moderateComment(operatorId, commentId, reason, ContentStatus.HIDDEN,
                ContentStatus.NORMAL, (byte) 0, "RESTORE");
    }

    @Override
    @Transactional
    public void deleteComment(Long operatorId, Long commentId, String reason) {
        BBSPostComment comment = requireComment(commentId);
        if (comment.getContentStatus() != ContentStatus.HIDDEN) {
            throw invalidTransition("评论必须先隐藏才能删除");
        }
        moderateComment(operatorId, commentId, reason, ContentStatus.HIDDEN,
                ContentStatus.DELETED, (byte) 1, "DELETE");
    }

    @Override
    @Transactional
    public void disableUser(Long operatorId, Long userId, String reason) {
        changeUserStatus(operatorId, userId, reason, (byte) 0, (byte) 1);
    }

    @Override
    @Transactional
    public void restoreUser(Long operatorId, Long userId, String reason) {
        changeUserStatus(operatorId, userId, reason, (byte) 1, (byte) 0);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(Long operatorId, CategoryRequest request) {
        BBSPostCategory category = new BBSPostCategory();
        category.setCategoryName(request.name().trim());
        category.setCategoryRank(request.rank() == null ? 0 : request.rank());
        category.setEnabled(true);
        category.setCreateTime(new Date());
        requireWrite(categoryMapper.insert(category), "分类新增失败");
        return toCategoryResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long operatorId, Integer categoryId,
                                           CategoryRequest request) {
        BBSPostCategory category = requireCategory(categoryId);
        category.setCategoryName(request.name().trim());
        category.setCategoryRank(request.rank() == null ? 0 : request.rank());
        requireWrite(categoryMapper.updateById(category), "分类修改失败");
        return toCategoryResponse(category);
    }

    @Override
    @Transactional
    public void setCategoryEnabled(Long operatorId, Integer categoryId, boolean enabled) {
        BBSPostCategory category = requireCategory(categoryId);
        category.setEnabled(enabled);
        requireWrite(categoryMapper.updateById(category), "分类状态修改失败");
    }

    private void moderatePost(Long operatorId, Long postId, String reason,
                              ContentStatus expected, ContentStatus target,
                              byte deleted, String action) {
        requireReason(reason);
        BBSPost post = requirePost(postId);
        if (post.getContentStatus() != expected) {
            throw invalidTransition("帖子状态不允许执行该操作");
        }
        requireAtomicWrite(postMapper.updateModerationStatus(
                postId, expected, target, deleted));
        record(operatorId, "POST", postId, action,
                expected, target, reason.trim());
    }

    private void moderateComment(Long operatorId, Long commentId, String reason,
                                 ContentStatus expected, ContentStatus target,
                                 byte deleted, String action) {
        requireReason(reason);
        BBSPostComment comment = requireComment(commentId);
        if (comment.getContentStatus() != expected) {
            throw invalidTransition("评论状态不允许执行该操作");
        }
        requireAtomicWrite(commentMapper.updateModerationStatus(
                commentId, expected, target, deleted));
        record(operatorId, "COMMENT", commentId, action,
                expected, target, reason.trim());
    }

    private void changeUserStatus(Long operatorId, Long userId, String reason,
                                  byte expected, byte target) {
        requireReason(reason);
        int updated = userMapper.updateStatusIfCurrent(userId, expected, target);
        if (updated <= 0) {
            BBSUser user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
            }
            throw invalidTransition("用户状态不允许执行该操作");
        }
    }

    private BBSPost requirePost(Long postId) {
        BBSPost post = postMapper.selectPostIncludingDeleted(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    private BBSPostComment requireComment(Long commentId) {
        BBSPostComment comment = commentMapper.selectIncludingDeleted(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "评论不存在");
        }
        return comment;
    }

    private BBSPostCategory requireCategory(Integer categoryId) {
        BBSPostCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private void record(Long operatorId, String targetType, Long targetId,
                        String action, ContentStatus previousStatus,
                        ContentStatus resultingStatus, String reason) {
        ModerationRecord record = new ModerationRecord();
        record.setOperatorUserId(operatorId);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setAction(action);
        record.setPreviousStatus(previousStatus);
        record.setResultingStatus(resultingStatus);
        record.setReason(reason);
        record.setCreateTime(new Date());
        moderationRecordMapper.insert(record);
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(
                    ErrorCode.MODERATION_REASON_REQUIRED, "操作原因不能为空");
        }
    }

    private void requireAtomicWrite(int updated) {
        if (updated <= 0) {
            throw new BusinessException(
                    ErrorCode.OPTIMISTIC_LOCK_CONFLICT, "内容状态已发生变化");
        }
    }

    private void requireWrite(int updated, String message) {
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private BusinessException invalidTransition(String message) {
        return new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, message);
    }

    private AdminPostFilterRequest normalize(AdminPostFilterRequest request) {
        return new AdminPostFilterRequest(
                request.page() == null ? 1 : request.page(),
                request.size() == null ? 20 : request.size(), request.status(),
                request.categoryId(), request.authorId(), request.from(), request.to());
    }

    private AdminCommentFilterRequest normalize(AdminCommentFilterRequest request) {
        return new AdminCommentFilterRequest(
                request.page() == null ? 1 : request.page(),
                request.size() == null ? 20 : request.size(), request.status(),
                request.postId(), request.authorId(), request.from(), request.to());
    }

    private PostResponse toPostResponse(BBSPost post) {
        return new PostResponse(post.getPostId(), post.getPublishUserId(),
                post.getNickName(), post.getPostCategoryId(), post.getPostCategoryName(),
                post.getPostTitle(), post.getPostContent(), post.getContentStatus(),
                post.getPostViews(), post.getPostComments(), post.getPostCollects(),
                post.getVersion() == null ? null : post.getVersion().longValue(),
                instant(post.getCreateTime()), instant(post.getLastUpdateTime()));
    }

    private CommentResponse toCommentResponse(BBSPostComment comment) {
        return new CommentResponse(comment.getCommentId(), comment.getPostId(),
                comment.getCommentUserId(), comment.getNickName(),
                comment.getParentCommentUserId(), comment.getCommentBody(),
                comment.getContentStatus(),
                comment.getVersion() == null ? null : comment.getVersion().longValue(),
                instant(comment.getCommentCreateTime()));
    }

    private CategoryResponse toCategoryResponse(BBSPostCategory category) {
        return new CategoryResponse(category.getCategoryId(), category.getCategoryName(),
                category.getCategoryRank(), Boolean.TRUE.equals(category.getEnabled()));
    }

    private Instant instant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
