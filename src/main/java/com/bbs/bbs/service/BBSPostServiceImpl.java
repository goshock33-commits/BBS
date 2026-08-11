package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.*;
import com.bbs.bbs.common.Constants;
import com.bbs.util.PageQueryUtil;
import com.bbs.util.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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
    @Resource
    private BBSPostMapper bbsPostMapper;
    @Resource
    private BBSUserMapper bbsUserMapper;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private com.bbs.bbs.bean.otherUtil otherUtil;

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
        Wrapper.eq(categoryName != null, BBSPost::getPostCategoryName, categoryName).eq(BBSPost::getPostStatus, 1).eq(BBSPost::getIsDeleted, 0);
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
        Wrapper.eq(categoryName != null, BBSPost::getPostCategoryName, categoryName).eq(BBSPost::getPostStatus, 1).eq(BBSPost::getIsDeleted, 0);
        return bbsPostMapper.selectCount(Wrapper);
    }

    @Override
    public BBSPost getBBSPostById(Long postId) {
        BBSPost bbsPost = (BBSPost) redisUtil.get("post:" + postId);
        if (bbsPost == null) return bbsPostMapper.selectById(postId);
        return bbsPost;
    }

    @Override
    public BBSPost getBBSPostForDetail(Long postId, Long userId) {
        //从redis中获取帖子
        BBSPost post = (BBSPost) redisUtil.get("post:" + postId);
        if (post == null) {
            //从数据库中查询帖子
            post = bbsPostMapper.selectById(postId);
            if (post == null) {
                return null;
            }
        }
        //从redis中获取用户
        BBSUser user = (BBSUser) redisUtil.get("user:" + post.getPublishUserId());
        if (user == null) {
            user = bbsUserMapper.selectById(post.getPublishUserId());
            redisUtil.set("user:" + user.getUserId(), user);
        }
        //从redis中获取用户
        //合并帖子和用户信息
        boolean isView = redisUtil.sHasKey("postViewed:" + postId, userId);
        if (!isView) {
            redisUtil.sSet("postViewed:" + postId, userId);
            post.setPostViews(post.getPostViews() + 1);
        }
        post.setNickName(user.getNickName());
        post.setHeadImgUrl(user.getHeadImgUrl());
        post.setUser(user);
        //存入redis
        redisUtil.set("post:" + post.getPostId(), post);
        bbsPostMapper.updateById(post);
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
        Wrapper.eq(BBSPost::getPostStatus, 1).eq(BBSPost::getIsDeleted, 0).ge(BBSPost::getCreateTime, calendar.getTime());
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
            //存入redis
            if (redisUtil.get("post:" + Post.getPostId()) == null) {
                redisUtil.set("post:" + Post.getPostId(), Post);
            }
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
        Wrapper.eq(BBSPost::getPostStatus, 1);
        Wrapper.ge(BBSPost::getCreateTime, calendar.getTime());
        Wrapper.orderByDesc(BBSPost::getCreateTime);
        return bbsPostMapper.selectList(Wrapper);
    }

    @Override
    public List<BBSPost> getMyBBSPostList(Long userId) {
        LambdaQueryWrapper<BBSPost> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSPost::getPublishUserId, userId);
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
        wrapper.eq(BBSPost::getPostStatus, 1)
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
}

