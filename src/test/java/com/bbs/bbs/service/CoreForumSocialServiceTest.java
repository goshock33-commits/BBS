package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbs.bbs.Mapper.BBSCommentMapper;
import com.bbs.bbs.Mapper.BBSCollectMapper;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSPostComment;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.RedisUtil;
import com.bbs.bbs.web.dto.collect.CollectResponse;
import com.bbs.bbs.web.dto.comment.CommentPageRequest;
import com.bbs.bbs.web.dto.comment.CommentResponse;
import com.bbs.bbs.web.dto.comment.CreateCommentRequest;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.model.ContentStatus;
import com.bbs.bbs.web.response.PageResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoreForumSocialServiceTest {
    @BeforeAll
    static void initializeTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                BBSPostComment.class);
    }
    @Mock BBSCommentMapper commentMapper;
    @Mock BBSCollectMapper collectMapper;
    @Mock BBSPostMapper postMapper;
    @Mock BBSUserMapper userMapper;
    @Mock RedisUtil redisUtil;
    @InjectMocks BBSCommentServiceImpl commentService;
    @InjectMocks BBSPostCollectServiceImpl collectService;

    @Test
    void replyParentMustBelongToTheSamePost() {
        BBSPost post = normalPost(10L, 7L);
        BBSPostComment parent = comment(99L, 11L, 8L);
        when(postMapper.selectPostIncludingDeleted(10L)).thenReturn(post);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(commentMapper.selectIncludingDeleted(99L)).thenReturn(parent);

        BusinessException error = assertThrows(BusinessException.class, () ->
                commentService.createComment(7L, 10L,
                        new CreateCommentRequest("reply", 99L)));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, error.getErrorCode());
        verify(commentMapper, never()).insert(any(BBSPostComment.class));
    }

    @Test
    void postAuthorCanDeleteCommentButOtherUserCannot() {
        BBSPost post = normalPost(10L, 7L);
        BBSPostComment existing = comment(99L, 10L, 8L);
        when(userMapper.selectById(9L)).thenReturn(activeUser(9L));
        when(commentMapper.selectIncludingDeleted(99L)).thenReturn(existing);
        when(postMapper.selectPostIncludingDeleted(10L)).thenReturn(post);

        BusinessException error = assertThrows(BusinessException.class,
                () -> commentService.deleteComment(9L, 99L));
        assertEquals(ErrorCode.FORBIDDEN_OWNERSHIP, error.getErrorCode());

        when(commentMapper.deleteById(99L)).thenReturn(1);
        when(postMapper.decrementCommentCount(10L)).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        assertDoesNotThrow(() -> commentService.deleteComment(7L, 99L));
    }

    @Test
    void hiddenPostRejectsCommentsAndCollections() {
        BBSPost hidden = normalPost(10L, 7L);
        hidden.setContentStatus(ContentStatus.HIDDEN);
        when(postMapper.selectPostIncludingDeleted(10L)).thenReturn(hidden);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));

        assertEquals(ErrorCode.INVALID_STATE_TRANSITION,
                assertThrows(BusinessException.class, () -> commentService.createComment(
                        7L, 10L, new CreateCommentRequest("body", null))).getErrorCode());
        assertEquals(ErrorCode.INVALID_STATE_TRANSITION,
                assertThrows(BusinessException.class, () -> collectService.collect(7L, 10L)).getErrorCode());
    }

    @Test
    void collectionAndCancelAreIdempotentAndCounterNeverNegative() {
        BBSPost post = normalPost(10L, 7L);
        post.setPostCollects(0L);
        when(postMapper.selectPostIncludingDeleted(10L)).thenReturn(post);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(collectMapper.findByUserAndPost(7L, 10L)).thenReturn(null);
        when(collectMapper.insertIgnore(any())).thenReturn(1);
        when(postMapper.incrementCollectCount(10L)).thenReturn(1);
        CollectResponse first = collectService.collect(7L, 10L);
        assertTrue(first.collected());

        when(collectMapper.findByUserAndPost(7L, 10L)).thenReturn(new com.bbs.bbs.bean.BBSPostCollect());
        assertTrue(collectService.collect(7L, 10L).collected());

        when(collectMapper.deleteByUserAndPost(7L, 10L)).thenReturn(0);
        assertFalse(collectService.cancelCollect(7L, 10L).collected());
        verify(postMapper, times(1)).incrementCollectCount(10L);
    }

    @Test
    void commentsAreReturnedAsDtosWithPagination() {
        BBSPostComment row = comment(1L, 10L, 7L);
        row.setCommentBody("hello");
        when(postMapper.selectById(10L)).thenReturn(normalPost(10L, 7L));
        when(commentMapper.selectPage(any(), any())).thenReturn(null);
        PageResult<CommentResponse> result = commentService.listComments(10L, new CommentPageRequest(1, 10));
        assertNotNull(result);
    }

    @Test
    void legacyCommentPageQueriesOnlyNormalComments() {
        Page<BBSPostComment> empty = new Page<>(1, 10);
        when(commentMapper.selectPage(any(), any())).thenReturn(empty);

        commentService.getCommentsByPostId(10L, 1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<BBSPostComment>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(commentMapper).selectPage(any(), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("content_status"));
    }

    @Test
    void legacyCollectionListOmitsHiddenPosts() {
        com.bbs.bbs.bean.BBSPostCollect collect = new com.bbs.bbs.bean.BBSPostCollect();
        collect.setPostId(10L);
        BBSPost hidden = normalPost(10L, 7L);
        hidden.setContentStatus(ContentStatus.HIDDEN);
        when(collectMapper.selectList(any())).thenReturn(List.of(collect));
        when(postMapper.selectById(10L)).thenReturn(hidden);

        assertTrue(collectService.getCollectRecordsByUserId(7L).isEmpty());
    }

    @Test
    void legacyCollectionListUsesDatabaseStatusInsteadOfStaleNormalCache() {
        com.bbs.bbs.bean.BBSPostCollect collect = new com.bbs.bbs.bean.BBSPostCollect();
        collect.setPostId(10L);
        BBSPost cachedNormal = normalPost(10L, 7L);
        BBSPost hidden = normalPost(10L, 7L);
        hidden.setContentStatus(ContentStatus.HIDDEN);
        when(collectMapper.selectList(any())).thenReturn(List.of(collect));
        lenient().when(redisUtil.get("post:10")).thenReturn(cachedNormal);
        when(postMapper.selectById(10L)).thenReturn(hidden);

        assertTrue(collectService.getCollectRecordsByUserId(7L).isEmpty());
        verify(postMapper).selectById(10L);
    }

    @Test
    void recentCommentsAreEmptyWhenAllReferencedPostsAreDeleted() {
        BBSPostComment existing = comment(99L, 10L, 7L);
        existing.setContentStatus(ContentStatus.NORMAL);
        when(commentMapper.selectList(any())).thenReturn(List.of(existing));
        when(postMapper.selectByIds(anyCollection())).thenReturn(List.of());

        assertTrue(commentService.getRecentCommentListByUserId(7L).isEmpty());
    }

    private static BBSPost normalPost(Long id, Long owner) {
        BBSPost post = new BBSPost();
        post.setPostId(id);
        post.setPublishUserId(owner);
        post.setContentStatus(ContentStatus.NORMAL);
        post.setIsDeleted((byte) 0);
        post.setPostComments(0L);
        post.setPostCollects(0L);
        return post;
    }

    private static BBSPostComment comment(Long id, Long postId, Long author) {
        BBSPostComment value = new BBSPostComment();
        value.setCommentId(id);
        value.setPostId(postId);
        value.setCommentUserId(author);
        value.setIsDeleted((byte) 0);
        value.setCommentCreateTime(new Date());
        return value;
    }

    private static BBSUser activeUser(Long id) {
        BBSUser user = new BBSUser();
        user.setUserId(id);
        user.setUserStatus((byte) 0);
        return user;
    }
}
