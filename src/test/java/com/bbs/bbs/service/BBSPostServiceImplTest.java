package com.bbs.bbs.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.util.PageQueryUtil;
import com.bbs.util.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BBSPostServiceImplTest {

    @Mock
    BBSPostMapper postMapper;

    @Mock
    BBSUserMapper userMapper;

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

}
