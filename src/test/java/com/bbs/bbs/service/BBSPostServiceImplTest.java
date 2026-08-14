package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbs.bbs.Mapper.BBSCategoryMapper;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.RedisUtil;
import com.bbs.bbs.bean.otherUtil;
import com.bbs.bbs.web.model.ContentStatus;
import com.bbs.util.PageQueryUtil;
import com.bbs.util.PageResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BBSPostServiceImplTest {

    @BeforeAll
    static void initializeTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                BBSPost.class);
    }

    @Mock
    BBSPostMapper postMapper;

    @Mock
    BBSUserMapper userMapper;

    @Mock
    BBSCategoryMapper categoryMapper;

    @Mock
    RedisUtil redisUtil;

    @Mock
    otherUtil otherUtil;

    @InjectMocks
    BBSPostServiceImpl service;

    @Test
    void getIndexPageReturnsEmptyPageInsteadOfNull() {
        Page<BBSPost> empty = new Page<>(1, 10);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(empty);

        PageResult result = service.getBBSPostPageForIndex(
                new PageQueryUtil(Map.of("page", 1, "limit", 10)));

        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void getIndexPageCapsPageSizeAndEnrichesAuthor() {
        BBSPost post = new BBSPost();
        post.setPostId(10L);
        post.setPublishUserId(20L);
        Page<BBSPost> page = new Page<>(2, 50);
        page.setRecords(List.of(post));
        page.setTotal(1);

        BBSUser user = new BBSUser();
        user.setUserId(20L);
        user.setNickName("Alice");
        user.setHeadImgUrl("/avatar/alice.png");
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userMapper.selectByIds(anyCollection())).thenReturn(List.of(user));

        PageResult result = service.getBBSPostPageForIndex(
                new PageQueryUtil(Map.of("page", 2, "limit", 100)));

        BBSPost enriched = (BBSPost) result.getList().get(0);
        assertEquals(50, result.getPageSize());
        assertEquals("Alice", enriched.getNickName());
        assertEquals("/avatar/alice.png", enriched.getHeadImgUrl());
        assertEquals(user, enriched.getUser());
    }

    @Test
    void legacyIndexPageQueriesOnlyNormalPosts() {
        Page<BBSPost> empty = new Page<>(1, 10);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(empty);

        service.getBBSPostPageForIndex(
                new PageQueryUtil(Map.of("page", 1, "limit", 10)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<BBSPost>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(postMapper)
                .selectPage(any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("content_status"));
    }

    @Test
    void legacyDetailUsesDatabaseStatusInsteadOfStaleNormalCache() {
        BBSPost cached = post(11L, 7L, ContentStatus.NORMAL);
        BBSPost hidden = post(11L, 7L, ContentStatus.HIDDEN);
        org.mockito.Mockito.lenient().when(redisUtil.get("post:11")).thenReturn(cached);
        org.mockito.Mockito.lenient().when(postMapper.selectPostIncludingDeleted(11L))
                .thenReturn(hidden);
        assertNull(service.getBBSPostForDetail(11L, 8L));
    }

    @Test
    void myCenterIncludesAuthorsHiddenPostsButExcludesDeletedPosts() {
        when(postMapper.selectList(any())).thenReturn(List.of());

        service.getMyBBSPostList(7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<BBSPost>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(postMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<BBSPost> wrapper = wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("content_status IN"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(ContentStatus.NORMAL));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(ContentStatus.HIDDEN));
    }

    @Test
    void hiddenPostOwnerViewDoesNotIncreaseViewsOrCachePost() {
        BBSPost hidden = post(11L, 7L, ContentStatus.HIDDEN);
        BBSUser author = new BBSUser();
        author.setUserId(7L);
        when(postMapper.selectPostIncludingDeleted(11L)).thenReturn(hidden);
        when(redisUtil.get("user:7")).thenReturn(author);

        assertNotNull(service.getBBSPostForDetail(11L, 7L));

        assertEquals(0L, hidden.getPostViews());
        verify(postMapper, never()).updateById(any(BBSPost.class));
        verify(redisUtil, never()).set("post:11", hidden);
    }

    @Test
    void publicUserProfileQueriesOnlyNormalPosts() {
        when(postMapper.selectList(any())).thenReturn(List.of());

        service.getRecentPostListByUserId(7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<BBSPost>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<BBSPost> wrapper = wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("content_status"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(ContentStatus.NORMAL));
    }

    private BBSPost post(long id, long authorId, ContentStatus status) {
        BBSPost post = new BBSPost();
        post.setPostId(id);
        post.setPublishUserId(authorId);
        post.setContentStatus(status);
        post.setPostViews(0L);
        return post;
    }

}
