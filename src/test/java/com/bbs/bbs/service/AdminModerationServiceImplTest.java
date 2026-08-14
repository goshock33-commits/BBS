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
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.model.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceImplTest {
    @Mock
    BBSPostMapper postMapper;
    @Mock
    BBSCommentMapper commentMapper;
    @Mock
    BBSUserMapper userMapper;
    @Mock
    BBSCategoryMapper categoryMapper;
    @Mock
    ModerationRecordMapper moderationRecordMapper;

    private AdminModerationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminModerationServiceImpl(postMapper, commentMapper,
                userMapper, categoryMapper, moderationRecordMapper);
    }

    @Test
    void hidingNormalPostUpdatesAtomicallyAndRecordsOperatorReason() {
        BBSPost post = post(41L, ContentStatus.NORMAL);
        when(postMapper.selectPostIncludingDeleted(41L)).thenReturn(post);
        when(postMapper.updateModerationStatus(41L, ContentStatus.NORMAL,
                ContentStatus.HIDDEN, (byte) 0)).thenReturn(1);

        service.hidePost(9L, 41L, "违反版规");

        verify(postMapper).updateModerationStatus(41L, ContentStatus.NORMAL,
                ContentStatus.HIDDEN, (byte) 0);
        ArgumentCaptor<ModerationRecord> record =
                ArgumentCaptor.forClass(ModerationRecord.class);
        verify(moderationRecordMapper).insert(record.capture());
        assertEquals(9L, record.getValue().getOperatorUserId());
        assertEquals("POST", record.getValue().getTargetType());
        assertEquals(41L, record.getValue().getTargetId());
        assertEquals("HIDE", record.getValue().getAction());
        assertEquals("违反版规", record.getValue().getReason());
        assertEquals(ContentStatus.NORMAL, record.getValue().getPreviousStatus());
        assertEquals(ContentStatus.HIDDEN, record.getValue().getResultingStatus());
    }

    @Test
    void deletedPostCannotBeRestored() {
        when(postMapper.selectPostIncludingDeleted(41L))
                .thenReturn(post(41L, ContentStatus.DELETED));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.restorePost(9L, 41L, "误判"));

        assertEquals(ErrorCode.INVALID_STATE_TRANSITION, exception.getErrorCode());
        verify(postMapper, never()).updateModerationStatus(any(), any(), any(), any());
        verify(moderationRecordMapper, never()).insert(any(ModerationRecord.class));
    }

    @Test
    void deletingHiddenCommentRecordsDeleteAndMarksDeleted() {
        BBSPostComment comment = comment(51L, ContentStatus.HIDDEN);
        when(commentMapper.selectIncludingDeleted(51L)).thenReturn(comment);
        when(commentMapper.updateModerationStatus(51L, ContentStatus.HIDDEN,
                ContentStatus.DELETED, (byte) 1)).thenReturn(1);

        service.deleteComment(9L, 51L, "违规内容");

        verify(commentMapper).updateModerationStatus(51L, ContentStatus.HIDDEN,
                ContentStatus.DELETED, (byte) 1);
        verify(moderationRecordMapper).insert(any(ModerationRecord.class));
    }

    @Test
    void disablingAndRestoringUserAreConditionalWithoutContentAuditRows() {
        when(userMapper.updateStatusIfCurrent(7L, (byte) 0, (byte) 1)).thenReturn(1);
        when(userMapper.updateStatusIfCurrent(7L, (byte) 1, (byte) 0)).thenReturn(1);

        service.disableUser(9L, 7L, "封禁账号");
        service.restoreUser(9L, 7L, "申诉通过");

        verify(userMapper).updateStatusIfCurrent(7L, (byte) 0, (byte) 1);
        verify(userMapper).updateStatusIfCurrent(7L, (byte) 1, (byte) 0);
        verify(moderationRecordMapper, never())
                .insert(any(ModerationRecord.class));
    }

    @Test
    void summaryReadsAllThreeCounters() {
        when(userMapper.countAll()).thenReturn(11L);
        when(postMapper.countAll()).thenReturn(22L);
        when(commentMapper.countAll()).thenReturn(33L);

        var summary = service.summary();

        assertEquals(11L, summary.users());
        assertEquals(22L, summary.posts());
        assertEquals(33L, summary.comments());
    }

    @Test
    void categoryCreateMapsDtoAndCategoryUpdatePreservesEnabledState() {
        when(categoryMapper.insert(any(BBSPostCategory.class))).thenAnswer(invocation -> {
            BBSPostCategory category = invocation.getArgument(0);
            category.setCategoryId(3);
            return 1;
        });
        when(categoryMapper.selectById(3)).thenReturn(category(3, true));
        when(categoryMapper.updateById(any(BBSPostCategory.class))).thenReturn(1);

        var created = service.createCategory(9L,
                new com.bbs.bbs.web.dto.category.CategoryRequest("后端", 8));
        var updated = service.updateCategory(9L, 3,
                new com.bbs.bbs.web.dto.category.CategoryRequest("Java", 9));

        assertEquals("后端", created.name());
        assertEquals("Java", updated.name());
        assertEquals(true, updated.enabled());
        verify(categoryMapper).updateById(any(BBSPostCategory.class));
        verify(moderationRecordMapper, never())
                .insert(any(ModerationRecord.class));
    }

    @Test
    void adminListsIncludeRequestedStatesAndStablePagination() {
        BBSPost hiddenPost = post(41L, ContentStatus.HIDDEN);
        hiddenPost.setPostTitle("待复核帖子");
        BBSPostComment deletedComment = comment(51L, ContentStatus.DELETED);
        deletedComment.setCommentBody("已删除评论");
        var postFilter = new com.bbs.bbs.web.dto.admin.AdminPostFilterRequest(
                null, null, ContentStatus.HIDDEN, 3, 7L, null, null);
        var commentFilter = new com.bbs.bbs.web.dto.admin.AdminCommentFilterRequest(
                2, 10, ContentStatus.DELETED, 41L, 7L, null, null);
        when(postMapper.selectAdminPage(any(), eq(0), eq(10)))
                .thenReturn(java.util.List.of(hiddenPost));
        when(postMapper.countAdminPage(any())).thenReturn(1L);
        when(commentMapper.selectAdminPage(commentFilter, 10, 10))
                .thenReturn(java.util.List.of(deletedComment));
        when(commentMapper.countAdminPage(commentFilter)).thenReturn(1L);

        var posts = service.listPosts(postFilter);
        var comments = service.listComments(commentFilter);

        assertEquals(1, posts.page());
        assertEquals(10, posts.size());
        assertEquals(ContentStatus.HIDDEN, posts.list().get(0).status());
        assertEquals(2, comments.page());
        assertEquals(ContentStatus.DELETED, comments.list().get(0).status());
    }

    @Test
    void postDeleteAndCommentHideRestoreUseLegalTransitions() {
        when(postMapper.selectPostIncludingDeleted(41L))
                .thenReturn(post(41L, ContentStatus.HIDDEN));
        when(postMapper.updateModerationStatus(41L, ContentStatus.HIDDEN,
                ContentStatus.DELETED, (byte) 1)).thenReturn(1);
        when(commentMapper.selectIncludingDeleted(51L))
                .thenReturn(comment(51L, ContentStatus.NORMAL),
                        comment(51L, ContentStatus.HIDDEN));
        when(commentMapper.updateModerationStatus(51L, ContentStatus.NORMAL,
                ContentStatus.HIDDEN, (byte) 0)).thenReturn(1);
        when(commentMapper.updateModerationStatus(51L, ContentStatus.HIDDEN,
                ContentStatus.NORMAL, (byte) 0)).thenReturn(1);

        service.deletePost(9L, 41L, "严重违规");
        service.hideComment(9L, 51L, "不当言论");
        service.restoreComment(9L, 51L, "复核通过");

        verify(postMapper).updateModerationStatus(41L, ContentStatus.HIDDEN,
                ContentStatus.DELETED, (byte) 1);
        verify(commentMapper).updateModerationStatus(51L, ContentStatus.NORMAL,
                ContentStatus.HIDDEN, (byte) 0);
        verify(commentMapper).updateModerationStatus(51L, ContentStatus.HIDDEN,
                ContentStatus.NORMAL, (byte) 0);
    }

    @Test
    void categoryEnableAndDisablePreserveOtherFields() {
        BBSPostCategory category = category(3, false);
        when(categoryMapper.selectById(3)).thenReturn(category);
        when(categoryMapper.updateById(category)).thenReturn(1);

        service.setCategoryEnabled(9L, 3, true);

        assertEquals(true, category.getEnabled());
        verify(categoryMapper).updateById(category);
        verify(moderationRecordMapper, never())
                .insert(any(ModerationRecord.class));
    }

    @Test
    void normalContentMustBeHiddenBeforeDeletion() {
        when(postMapper.selectPostIncludingDeleted(41L))
                .thenReturn(post(41L, ContentStatus.NORMAL));
        when(commentMapper.selectIncludingDeleted(51L))
                .thenReturn(comment(51L, ContentStatus.NORMAL));
        BusinessException postException = assertThrows(BusinessException.class,
                () -> service.deletePost(9L, 41L, "直接删除"));
        BusinessException commentException = assertThrows(BusinessException.class,
                () -> service.deleteComment(9L, 51L, "直接删除"));

        assertEquals(ErrorCode.INVALID_STATE_TRANSITION,
                postException.getErrorCode());
        assertEquals(ErrorCode.INVALID_STATE_TRANSITION,
                commentException.getErrorCode());
    }

    private static BBSPost post(Long id, ContentStatus status) {
        BBSPost post = new BBSPost();
        post.setPostId(id);
        post.setContentStatus(status);
        post.setIsDeleted((byte) (status == ContentStatus.DELETED ? 1 : 0));
        return post;
    }

    private static BBSPostComment comment(Long id, ContentStatus status) {
        BBSPostComment comment = new BBSPostComment();
        comment.setCommentId(id);
        comment.setContentStatus(status);
        comment.setIsDeleted((byte) (status == ContentStatus.DELETED ? 1 : 0));
        return comment;
    }

    private static BBSPostCategory category(Integer id, boolean enabled) {
        BBSPostCategory category = new BBSPostCategory();
        category.setCategoryId(id);
        category.setCategoryName("旧名称");
        category.setCategoryRank(1);
        category.setEnabled(enabled);
        return category;
    }
}
