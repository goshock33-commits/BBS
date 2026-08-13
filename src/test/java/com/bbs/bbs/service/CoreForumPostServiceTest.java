package com.bbs.bbs.service;

import com.bbs.bbs.Mapper.BBSCategoryMapper;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSPostCategory;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.RedisUtil;
import com.bbs.bbs.bean.otherUtil;
import com.bbs.bbs.web.dto.post.CreatePostRequest;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.dto.post.UpdatePostRequest;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.model.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreForumPostServiceTest {

    @Mock BBSPostMapper postMapper;
    @Mock BBSUserMapper userMapper;
    @Mock BBSCategoryMapper categoryMapper;
    @Mock RedisUtil redisUtil;
    @Mock otherUtil otherUtil;

    private BBSPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BBSPostServiceImpl(
                postMapper, userMapper, categoryMapper, redisUtil, otherUtil);
    }

    @Test
    void createRejectsDisabledCategory() {
        BBSPostCategory category = category(3, false);
        when(categoryMapper.selectById(3)).thenReturn(category);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createPost(7L,
                        new CreatePostRequest(3, "标题", "正文", List.of())));

        assertEquals(ErrorCode.CATEGORY_DISABLED, exception.getErrorCode());
        verify(postMapper, never()).insert(any(BBSPost.class));
    }

    @Test
    void createDefaultsToNormalWithZeroCountersAndVersion() {
        when(categoryMapper.selectById(3)).thenReturn(category(3, true));
        when(postMapper.insert(any(BBSPost.class))).thenAnswer(invocation -> {
            invocation.<BBSPost>getArgument(0).setPostId(11L);
            return 1;
        });

        PostResponse response = service.createPost(7L,
                new CreatePostRequest(3, " 标题 ", " 正文 ", List.of()));

        ArgumentCaptor<BBSPost> captor = ArgumentCaptor.forClass(BBSPost.class);
        verify(postMapper).insert(captor.capture());
        BBSPost saved = captor.getValue();
        assertEquals(7L, saved.getPublishUserId());
        assertEquals(ContentStatus.NORMAL, saved.getContentStatus());
        assertEquals(0L, saved.getPostViews());
        assertEquals(0L, saved.getPostComments());
        assertEquals(0L, saved.getPostCollects());
        assertEquals(0, saved.getVersion());
        assertEquals(11L, response.id());
    }

    @Test
    void nonOwnerCannotEditPost() {
        BBSPost post = post(11L, 7L, ContentStatus.NORMAL, 2);
        when(postMapper.selectPostIncludingDeleted(11L)).thenReturn(post);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePost(8L, 11L,
                        new UpdatePostRequest(3, "标题", "正文", List.of(), 2L)));

        assertEquals(ErrorCode.FORBIDDEN_OWNERSHIP, exception.getErrorCode());
        verify(postMapper, never()).updateOwnedPost(any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    void optimisticLockConflictReturnsStableError() {
        BBSPost post = post(11L, 7L, ContentStatus.NORMAL, 2);
        when(postMapper.selectPostIncludingDeleted(11L)).thenReturn(post);
        when(categoryMapper.selectById(3)).thenReturn(category(3, true));
        when(postMapper.updateOwnedPost(11L, 7L, "标题", "正文", 3,
                "分类3", 2L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePost(7L, 11L,
                        new UpdatePostRequest(3, "标题", "正文", List.of(), 2L)));

        assertEquals(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, exception.getErrorCode());
    }

    @Test
    void hiddenPostIsVisibleOnlyToAuthorOrAdmin() {
        BBSPost post = post(11L, 7L, ContentStatus.HIDDEN, 1);
        when(postMapper.selectPostIncludingDeleted(11L)).thenReturn(post);

        assertThrows(BusinessException.class,
                () -> service.getVisiblePost(11L, 8L, false));
        assertEquals(ContentStatus.HIDDEN,
                service.getVisiblePost(11L, 7L, false).status());
        assertEquals(ContentStatus.HIDDEN,
                service.getVisiblePost(11L, 8L, true).status());
    }

    @Test
    void deletedPostIsVisibleOnlyToAdminAndOwnerCannotRestoreIt() {
        BBSPost post = post(11L, 7L, ContentStatus.DELETED, 3);
        when(postMapper.selectPostIncludingDeleted(11L)).thenReturn(post);

        assertThrows(BusinessException.class,
                () -> service.getVisiblePost(11L, 7L, false));
        assertEquals(ContentStatus.DELETED,
                service.getVisiblePost(11L, 8L, true).status());
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePost(7L, 11L,
                        new UpdatePostRequest(3, "标题", "正文", List.of(), 3L)));
        assertEquals(ErrorCode.INVALID_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void ownerDeleteIsIdempotent() {
        BBSPost post = post(11L, 7L, ContentStatus.NORMAL, 3);
        when(postMapper.selectPostIncludingDeleted(11L))
                .thenReturn(post)
                .thenReturn(post(11L, 7L, ContentStatus.DELETED, 4));
        when(postMapper.deleteOwnedPost(11L, 7L, 3L)).thenReturn(1);

        service.deletePost(7L, 11L);
        service.deletePost(7L, 11L);

        verify(postMapper).deleteOwnedPost(11L, 7L, 3L);
    }

    @Test
    void publicListReturnsNormalPostsWithAuthorName() {
        BBSPost post = post(11L, 7L, ContentStatus.NORMAL, 1);
        when(postMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<BBSPost> page =
                    invocation.getArgument(0);
            page.setRecords(List.of(post));
            page.setTotal(1);
            return page;
        });
        BBSUser author = new BBSUser();
        author.setUserId(7L);
        author.setNickName("作者");
        when(userMapper.selectByIds(any())).thenReturn(List.of(author));

        com.bbs.bbs.web.response.PageResult<PostResponse> result =
                service.listPublicPosts(new com.bbs.bbs.web.dto.post.PostPageRequest(
                        1, 10, null, null, null, null));

        assertEquals(1, result.list().size());
        assertEquals("作者", result.list().get(0).authorName());
    }

    private BBSPostCategory category(int id, boolean enabled) {
        BBSPostCategory category = new BBSPostCategory();
        category.setCategoryId(id);
        category.setCategoryName("分类" + id);
        category.setEnabled(enabled);
        return category;
    }

    private BBSPost post(long id, long authorId, ContentStatus status, int version) {
        BBSPost post = new BBSPost();
        post.setPostId(id);
        post.setPublishUserId(authorId);
        post.setPostTitle("标题");
        post.setPostContent("正文");
        post.setPostCategoryId(3);
        post.setPostCategoryName("分类3");
        post.setContentStatus(status);
        post.setPostViews(0L);
        post.setPostComments(0L);
        post.setPostCollects(0L);
        post.setVersion(version);
        return post;
    }
}
