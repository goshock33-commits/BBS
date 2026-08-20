package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSCategoryMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.*;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.web.dto.post.CreatePostRequest;
import com.bbs.bbs.web.dto.post.PostPageRequest;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.dto.post.UpdatePostRequest;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.model.ContentStatus;
import com.bbs.util.PageQueryUtil;
import com.bbs.util.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-27 14:45:57
 * @Version 1.0
 */
@Service
public class BBSPostServiceImpl implements BBSPostService {
    private final BBSPostMapper bbsPostMapper;
    private final BBSUserMapper bbsUserMapper;
    private final BBSCategoryMapper categoryMapper;
    private final RedisUtil redisUtil;
    private final com.bbs.bbs.bean.otherUtil otherUtil;

    public BBSPostServiceImpl(BBSPostMapper bbsPostMapper,
                              BBSUserMapper bbsUserMapper,
                              BBSCategoryMapper categoryMapper,
                              RedisUtil redisUtil,
                              com.bbs.bbs.bean.otherUtil otherUtil) {
        this.bbsPostMapper = bbsPostMapper;
        this.bbsUserMapper = bbsUserMapper;
        this.categoryMapper = categoryMapper;
        this.redisUtil = redisUtil;
        this.otherUtil = otherUtil;
    }

    @Override
    @Transactional
    public PostResponse createPost(Long currentUserId, CreatePostRequest request) {
        BBSPostCategory category = requireEnabledCategory(request.categoryId());
        BBSPost post = new BBSPost();
        post.setPublishUserId(currentUserId);
        post.setPostTitle(request.title().trim());
        post.setPostContent(request.content().trim());
        post.setPostCategoryId(category.getCategoryId());
        post.setPostCategoryName(category.getCategoryName());
        post.setPostStatus((byte) 1);
        post.setContentStatus(ContentStatus.NORMAL);
        post.setPostViews(0L);
        post.setPostComments(0L);
        post.setPostCollects(0L);
        post.setVersion(0);
        post.setIsDeleted((byte) 0);
        Date now = new Date();
        post.setLastUpdateTime(now);
        post.setCreateTime(now);
        if (bbsPostMapper.insert(post) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "发帖失败");
        }
        return toResponse(post);
    }

    @Override
    public PostResponse getVisiblePost(Long postId, Long currentUserId, boolean admin) {
        BBSPost post = requirePost(postId);
        boolean owner = currentUserId != null
                && currentUserId.equals(post.getPublishUserId());
        if (post.getContentStatus() == ContentStatus.DELETED && !admin) {
            throw notFound();
        }
        if (post.getContentStatus() == ContentStatus.HIDDEN && !owner && !admin) {
            throw notFound();
        }
        return toResponse(post);
    }

    @Override
    public com.bbs.bbs.web.response.PageResult<PostResponse> listPublicPosts(
            PostPageRequest request) {
        LambdaQueryWrapper<BBSPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BBSPost::getContentStatus, ContentStatus.NORMAL)
                .eq(BBSPost::getIsDeleted, 0)
                .eq(request.categoryId() != null,
                        BBSPost::getPostCategoryId, request.categoryId());
        if (request.keyword() != null && !request.keyword().isBlank()) {
            String keyword = request.keyword().trim();
            wrapper.and(query -> query.like(BBSPost::getPostTitle, keyword)
                    .or().like(BBSPost::getPostContent, keyword));
        }
        wrapper.orderByDesc(BBSPost::getCreateTime);
        IPage<BBSPost> page = bbsPostMapper.selectPage(
                new Page<>(request.page(), request.size()), wrapper);
        Map<Long, BBSUser> authors = page.getRecords().stream()
                .map(BBSPost::getPublishUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), ids ->
                        bbsUserMapper.selectByIds(ids).stream()
                                .collect(Collectors.toMap(BBSUser::getUserId,
                                        user -> user, (left, right) -> left))));
        List<PostResponse> responses = page.getRecords().stream()
                .map(post -> toResponse(post, authors.get(post.getPublishUserId())))
                .toList();
        return com.bbs.bbs.web.response.PageResult.of(
                responses, page.getTotal(), request.page(), request.size());
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long currentUserId, Long postId,
                                   UpdatePostRequest request) {
        BBSPost post = requirePost(postId);
        requireOwner(post, currentUserId);
        if (post.getContentStatus() != ContentStatus.NORMAL) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "当前帖子状态不允许编辑");
        }
        BBSPostCategory category = requireEnabledCategory(request.categoryId());
        int updated = bbsPostMapper.updateOwnedPost(
                postId, currentUserId, request.title().trim(), request.content().trim(),
                category.getCategoryId(), category.getCategoryName(), request.version());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "帖子已被其他操作修改，请刷新后重试");
        }
        post.setPostTitle(request.title().trim());
        post.setPostContent(request.content().trim());
        post.setPostCategoryId(category.getCategoryId());
        post.setPostCategoryName(category.getCategoryName());
        post.setVersion(Math.toIntExact(request.version() + 1));
        return toResponse(post, bbsUserMapper.selectById(post.getPublishUserId()));
    }

    @Override
    @Transactional
    public void deletePost(Long currentUserId, Long postId) {
        BBSPost post = requirePost(postId);
        requireOwner(post, currentUserId);
        if (post.getContentStatus() == ContentStatus.DELETED) {
            return;
        }
        if (bbsPostMapper.deleteOwnedPost(postId, currentUserId,
                post.getVersion().longValue()) == 0) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "帖子已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    public int addPost(BBSPost post, BBSUser user) {
        // 设置帖子作者
        post.setPublishUserId(user.getUserId());
        post.setPostStatus((byte) 1);
        post.setIsDeleted((byte) 0);
        post.setPostViews(0L);
        post.setPostComments(0L);
        post.setPostCollects(0L);
        post.setLastUpdateTime(new Date());
        // 设置创建时间
        post.setCreateTime(new Date());
        return bbsPostMapper.insert(post);
    }

    @Override
    public List<BBSPost> selectPostByCategoryName(String categoryName, Integer pageNum) {
        // 分页查询
        LambdaQueryWrapper<BBSPost> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(categoryName != null, BBSPost::getPostCategoryName, categoryName)
                .eq(BBSPost::getContentStatus, ContentStatus.NORMAL)
                .eq(BBSPost::getPostStatus, 1)
                .eq(BBSPost::getIsDeleted, 0);
        Wrapper.orderByDesc(BBSPost::getCreateTime);
        IPage<BBSPost> page = new Page<>(pageNum, 10);
        IPage<BBSPost> iPage = bbsPostMapper.selectPage(page, Wrapper);
        List<BBSPost> records = iPage.getRecords();
        List<Long> userIds = null;
        if (!records.isEmpty()) {
            userIds = records.stream().map(BBSPost::getPublishUserId).collect(Collectors.toList());
        } else {
            return null;
        }
        List<BBSUser> users = bbsUserMapper.selectByIds(userIds);
        Map<Long, BBSUser> userMap = null;
        // 合并帖子和用户信息
        // 优化：使用Map存储用户信息，避免重复查询数据库
        if (!users.isEmpty()) {
            userMap = users.stream().collect(Collectors.toMap(BBSUser::getUserId, user -> user, (a, b) -> a));
        } else {
            throw new IllegalArgumentException("查询失败");
        }
        for (BBSPost record : records) {
            BBSUser user = userMap.get(record.getPublishUserId());
            record.setNickName(user.getNickName());
            record.setHeadImgUrl(user.getHeadImgUrl());
            record.setUser(user);
            redisUtil.set("user:" + user.getUserId(), user);
            //存入redis
            redisUtil.set("post:" + record.getPostId(), record);
        }
        return records;
    }

    @Override
    public long getTotalPostCount(String categoryName) {
        LambdaQueryWrapper<BBSPost> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(categoryName != null, BBSPost::getPostCategoryName, categoryName)
                .eq(BBSPost::getContentStatus, ContentStatus.NORMAL)
                .eq(BBSPost::getPostStatus, 1)
                .eq(BBSPost::getIsDeleted, 0);
        return bbsPostMapper.selectCount(Wrapper);
    }

    @Override
    public BBSPost getBBSPostById(Long postId) {
        BBSPost post = bbsPostMapper.selectPostIncludingDeleted(postId);
        return post != null && post.getContentStatus() == ContentStatus.NORMAL
                ? post : null;
    }

    @Override
    public BBSPost getBBSPostForDetail(Long postId, Long userId) {
        BBSPost post = bbsPostMapper.selectPostIncludingDeleted(postId);
        if (post == null) {
            return null;
        }
        boolean owner = userId != null && userId.equals(post.getPublishUserId());
        if (post.getContentStatus() != ContentStatus.NORMAL
                && !(owner && post.getContentStatus() == ContentStatus.HIDDEN)) {
            return null;
        }
        BBSUser user = bbsUserMapper.selectById(post.getPublishUserId());
        if (user == null) {
            return null;
        }
        if (post.getContentStatus() == ContentStatus.HIDDEN) {
            post.setNickName(user.getNickName());
            post.setHeadImgUrl(user.getHeadImgUrl());
            post.setUser(user);
            return post;
        }
        int updated = bbsPostMapper.incrementViewCount(postId);
        if (updated > 0) {
            post.setPostViews((post.getPostViews() == null ? 0L : post.getPostViews()) + 1L);
        }
        post.setNickName(user.getNickName());
        post.setHeadImgUrl(user.getHeadImgUrl());
        post.setUser(user);
        return post;
    }

    @Override
    public int updateBBSPost(BBSPost bbsPost) {
        int rows = bbsPostMapper.updateById(bbsPost);
        if (rows > 0) {
            // 同步更新Redis缓存，防止详情页读取旧缓存覆盖数据库
            redisUtil.set("post:" + bbsPost.getPostId(), bbsPost);
        }
        return rows;
    }

    @Override
    public int delBBSPost(Long userId, Long postId) {
        //从redis中获取帖子
        BBSPost post = (BBSPost) redisUtil.get("post:" + postId);
        if (post == null) {
            post = bbsPostMapper.selectById(postId);
            if (post == null) {
                return 0;
            }
        }
        //从帖子作者中判断是否是当前用户
        if (!post.getPublishUserId().equals(userId)) {
            return 2;
        }
        //删除帖子
        post.setIsDeleted((byte) 1);
        bbsPostMapper.updateById(post);
        //删除redis中的帖子
        redisUtil.del("post:" + postId);
        return 1;
    }

    @Override
    public List<HotPost> getHotTopicBBSPostList() {
        //redis
        List<BBSPost> hotPosts = null;
        List<HotPost> hotPostList = new ArrayList<>();
        //从数据库中查询帖子
        LambdaQueryWrapper<BBSPost> Wrapper = new LambdaQueryWrapper<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        //查询24小时内的帖子
        Wrapper.eq(BBSPost::getContentStatus, ContentStatus.NORMAL)
                .eq(BBSPost::getPostStatus, 1)
                .eq(BBSPost::getIsDeleted, 0)
                .ge(BBSPost::getCreateTime, calendar.getTime());
        Wrapper.orderByDesc(BBSPost::getCreateTime);
        List<BBSPost> bbsPosts = bbsPostMapper.selectList(Wrapper);
        if (bbsPosts.isEmpty()) {
            return null;
        }
        hotPosts = bbsPosts.stream().sorted((post1, post2) -> {
            int score1 = otherUtil.calculateHotScore(post1);
            int score2 = otherUtil.calculateHotScore(post2);
            return score2 - score1;
        }).limit(10).toList();
        for (BBSPost Post : hotPosts) {
            HotPost hotPost = new HotPost();
            hotPost.setPostTitle(Post.getPostTitle());
            hotPost.setPostComments(Post.getPostComments());
            hotPost.setPostId(Post.getPostId());
            hotPostList.add(hotPost);
        }
        return hotPostList;
    }

    @Override
    public List<BBSPost> getRecentPostListByUserId(Long userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        LambdaQueryWrapper<BBSPost> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSPost::getPublishUserId, userId);
        Wrapper.eq(BBSPost::getContentStatus, ContentStatus.NORMAL);
        Wrapper.eq(BBSPost::getPostStatus, 1);
        Wrapper.ge(BBSPost::getCreateTime, calendar.getTime());
        Wrapper.orderByDesc(BBSPost::getCreateTime);
        return bbsPostMapper.selectList(Wrapper);
    }

    @Override
    public List<BBSPost> getMyBBSPostList(Long userId) {
        LambdaQueryWrapper<BBSPost> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSPost::getPublishUserId, userId);
        Wrapper.in(BBSPost::getContentStatus,
                ContentStatus.NORMAL, ContentStatus.HIDDEN);
        Wrapper.eq(BBSPost::getPostStatus, 1);
        Wrapper.eq(BBSPost::getIsDeleted, 0);
        Wrapper.orderByDesc(BBSPost::getCreateTime);
        return bbsPostMapper.selectList(Wrapper);
    }

    @Override
    public PageResult getBBSPostPageForIndex(PageQueryUtil pageUtil) {
        int pageNumber = pageUtil.getPage();
        int pageSize = Math.min(pageUtil.getLimit(), 50);
        LambdaQueryWrapper<BBSPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BBSPost::getContentStatus, ContentStatus.NORMAL)
                .eq(BBSPost::getPostStatus, 1)
                .eq(BBSPost::getIsDeleted, 0);

        Object categoryId = pageUtil.get("categoryId");
        if (categoryId != null) {
            wrapper.eq(BBSPost::getPostCategoryId,
                    Long.parseLong(categoryId.toString()));
        }

        Object keyword = pageUtil.get("keyword");
        if (keyword != null && !keyword.toString().isBlank()) {
            String value = keyword.toString().trim();
            wrapper.and(w -> w.like(BBSPost::getPostTitle, value)
                    .or().like(BBSPost::getPostContent, value));
        }

        Object period = pageUtil.get("period");
        if ("week".equals(period)) {
            wrapper.ge(BBSPost::getCreateTime,
                    java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(7)));
        } else if ("month".equals(period)) {
            wrapper.ge(BBSPost::getCreateTime,
                    java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(30)));
        }

        if ("views".equals(pageUtil.get("orderBy"))) {
            wrapper.orderByDesc(BBSPost::getPostViews);
        } else {
            wrapper.orderByDesc(BBSPost::getCreateTime);
        }

        IPage<BBSPost> page = bbsPostMapper.selectPage(
                new Page<>(pageNumber, pageSize), wrapper);
        List<BBSPost> records = page.getRecords();
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream()
                    .map(BBSPost::getPublishUserId)
                    .distinct()
                    .toList();
            Map<Long, BBSUser> users = bbsUserMapper.selectByIds(userIds).stream()
                    .collect(Collectors.toMap(BBSUser::getUserId, user -> user));
            records.forEach(post -> {
                BBSUser user = users.get(post.getPublishUserId());
                if (user != null) {
                    post.setNickName(user.getNickName());
                    post.setHeadImgUrl(user.getHeadImgUrl());
                    post.setUser(user);
                }
            });
        }
        return new PageResult(records, Math.toIntExact(page.getTotal()),
                pageSize, pageNumber);
    }

    private BBSPostCategory requireEnabledCategory(Integer categoryId) {
        BBSPostCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
        }
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new BusinessException(ErrorCode.CATEGORY_DISABLED, "分类已停用");
        }
        return category;
    }

    private BBSPost requirePost(Long postId) {
        BBSPost post = bbsPostMapper.selectPostIncludingDeleted(postId);
        if (post == null) {
            throw notFound();
        }
        return post;
    }

    private void requireOwner(BBSPost post, Long currentUserId) {
        if (currentUserId == null
                || !currentUserId.equals(post.getPublishUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_OWNERSHIP,
                    "只能操作自己的帖子");
        }
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "帖子不存在");
    }

    private PostResponse toResponse(BBSPost post) {
        return toResponse(post, null);
    }

    private PostResponse toResponse(BBSPost post, BBSUser author) {
        java.time.Instant created = post.getCreateTime() == null
                ? null : post.getCreateTime().toInstant();
        java.time.Instant updated = post.getLastUpdateTime() == null
                ? null : post.getLastUpdateTime().toInstant();
        return new PostResponse(
                post.getPostId(),
                post.getPublishUserId(),
                author == null ? post.getNickName() : author.getNickName(),
                post.getPostCategoryId(),
                post.getPostCategoryName(),
                post.getPostTitle(),
                post.getPostContent(),
                post.getContentStatus(),
                post.getPostViews(),
                post.getPostComments(),
                post.getPostCollects(),
                post.getVersion() == null ? null : post.getVersion().longValue(),
                created,
                updated);
    }
}

