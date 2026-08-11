package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbs.bbs.Mapper.BBSCommentMapper;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.*;
import com.bbs.util.BeanUtil;
import com.bbs.util.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BBSCommentServiceImpl implements BBSCommentService {

    @Resource
    private BBSCommentMapper bbsCommentMapper;
    @Resource
    private BBSUserMapper bbsUserMapper;
    @Resource
    private BBSPostMapper bbsPostMapper;
    @Resource
    private RedisUtil redisUtil;

    @Override
    @Transactional
    public Boolean addPostComment(BBSPostComment postComment) {
        // 1. 校验帖子是否存在
        BBSPost bbsPost = (BBSPost) redisUtil.get("post:" + postComment.getPostId().toString());
        if (bbsPost == null) {
            bbsPost = bbsPostMapper.selectById(postComment.getPostId());
            redisUtil.set("post:" + postComment.getPostId().toString(), bbsPost);
            if (bbsPost == null) {
                return false;
            }
        }
        // 2. 校验评论人状态
        BBSUser bbsUser = (BBSUser) redisUtil.get("user:" + postComment.getCommentUserId().toString());
        if (bbsUser == null) {
            bbsUser = bbsUserMapper.selectById(postComment.getCommentUserId());
            redisUtil.set("user:" + postComment.getCommentUserId().toString(), bbsUser);
            if (bbsUser == null || bbsUser.getUserStatus().intValue() == 1) {
                return false;
            }
        }
        // 3. 帖子评论数 +1
        bbsPost.setPostComments(bbsPost.getPostComments() + 1);
        // 4. 插入评论 & 更新帖子（事务保证一致性）
        return bbsCommentMapper.insert(postComment) > 0 && bbsPostMapper.updateById(bbsPost) > 0&&redisUtil.set("post:"+postComment.getPostId().toString(),bbsPost);
    }

    @Override
    @Transactional
    public Boolean delPostComment(Long commentId, Long userId) {
        BBSPostComment bbsPostComment = (BBSPostComment) redisUtil.get("comment:" + commentId.toString());
        if (bbsPostComment == null) {
            bbsPostComment = bbsCommentMapper.selectById(commentId);
            redisUtil.set("comment:" + commentId.toString(), bbsPostComment);
            if (bbsPostComment == null) {
                return false;
            }
        }
        BBSUser bbsUser = (BBSUser) redisUtil.get("user:" + userId.toString());
        if (bbsUser == null) {
            bbsUser = bbsUserMapper.selectById(userId);
            redisUtil.set("user:" + userId.toString(), bbsUser);
            if (bbsUser == null || bbsUser.getUserStatus().intValue() == 1) {
                return false;
            }
        }
        BBSPost bbsPost = (BBSPost) redisUtil.get("post:" + bbsPostComment.getPostId().toString());
        if (bbsPost == null) {
            bbsPost = bbsPostMapper.selectById(bbsPostComment.getPostId());
            if (bbsPost != null) redisUtil.set("post:" + bbsPostComment.getPostId().toString(), bbsPost);
            if (bbsPost == null) {
                return false;
            }
        }
        // 评论数 -1（防止负数）
        Long commentCount = bbsPost.getPostComments() - 1;
        if (commentCount >= 0) {
            bbsPost.setPostComments(commentCount);
        }
        // 评论者本人 或 帖子作者 都可以删除评论
        if (userId.equals(bbsPostComment.getCommentUserId()) || userId.equals(bbsPost.getPublishUserId())) {
            return bbsCommentMapper.deleteById(commentId) > 0 && bbsPostMapper.updateById(bbsPost) > 0&&redisUtil.set("post:"+bbsPostComment.getPostId().toString(),bbsPost);
        }
        return false;
    }

    @Override
    public PageResult getCommentsByPostId(Long postId, int page) {
        LambdaQueryWrapper<BBSPostComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BBSPostComment::getPostId, postId).eq(BBSPostComment::getIsDeleted, 0).orderByAsc(BBSPostComment::getCommentCreateTime);

        IPage<BBSPostComment> iPage = bbsCommentMapper.selectPage(new Page<>(page, 10), wrapper);
        List<BBSPostComment> records = iPage.getRecords();

        if (!records.isEmpty()) {
            List<Long> userIds = records.stream().map(BBSPostComment::getCommentUserId).distinct().collect(Collectors.toList());
            List<BBSUser> users = bbsUserMapper.selectByIds(userIds);
            Map<Long, BBSUser> userMap = users.stream().collect(Collectors.toMap(BBSUser::getUserId, u -> u, (a, b) -> a));
            for (BBSPostComment comment : records) {
                BBSUser user = userMap.get(comment.getCommentUserId());
                if (user != null) {
                    comment.setNickName(user.getNickName());
                    comment.setHeadImgUrl(user.getHeadImgUrl());
                    comment.setUser(user);
                }
            }
        }
        return new PageResult(records, (int) iPage.getTotal(), 10, page);
    }

    @Override
    public List<RecentCommentListEntity> getRecentCommentListByUserId(Long userId) {
        LambdaQueryWrapper<BBSPostComment> wrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<BBSPostComment> listWrapper = wrapper.eq(BBSPostComment::getCommentUserId, userId).eq(BBSPostComment::getIsDeleted, 0).orderByAsc(BBSPostComment::getCommentCreateTime).gt(BBSPostComment::getCommentCreateTime, LocalDateTime.now().minusDays(7)).last("limit 6");
        List<RecentCommentListEntity> recentCommentListEntities = new ArrayList<>();
        List<BBSPostComment> recentCommentList = bbsCommentMapper.selectList(listWrapper);
        if (!CollectionUtils.isEmpty(recentCommentList)) {
            // 复制评论列表到实体列表
            recentCommentListEntities = BeanUtil.copyList(recentCommentList, RecentCommentListEntity.class);
            // 收集帖子 ID，批量查询帖子标题
            List<Long> postIds = recentCommentList.stream().map(BBSPostComment::getPostId).collect(Collectors.toList());
            List<BBSPost> bbsPosts = bbsPostMapper.selectByIds(postIds);
            if (!CollectionUtils.isEmpty(bbsPosts)) {
                Map<Long, BBSPost> bbsPostMap = bbsPosts.stream().collect(Collectors.toMap(BBSPost::getPostId, Function.identity(), (e1, e2) -> e1));
                for (RecentCommentListEntity entity : recentCommentListEntities) {
                    if (bbsPostMap.containsKey(entity.getPostId())) {
                        entity.setPostTitle(bbsPostMap.get(entity.getPostId()).getPostTitle());
                    }
                }
            }
        }
        return recentCommentListEntities;
    }
}