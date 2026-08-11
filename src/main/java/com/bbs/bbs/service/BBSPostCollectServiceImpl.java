package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbs.bbs.Mapper.BBSCollectMapper;
import com.bbs.bbs.Mapper.BBSPostMapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSPostCollect;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.RedisUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-05-12 16:03:54
 * @Version 1.0
 */
@Service
public class BBSPostCollectServiceImpl implements BBSPostCollectService{
    @Resource
    private BBSCollectMapper collectMapper;
    @Resource
    private BBSPostMapper postMapper;
    @Resource
    private BBSUserMapper userMapper;
    @Resource
    private RedisUtil redisUtil;
    @Override
    @Transactional
    public Boolean addCollectRecord(Long userId, Long postId) {
        LambdaQueryWrapper<BBSPostCollect> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSPostCollect::getUserId, userId);
        Wrapper.eq(BBSPostCollect::getPostId, postId);
        BBSPostCollect collect = collectMapper.selectOne(Wrapper);
        BBSUser user = userMapper.selectById(userId);
        BBSPost post = postMapper.selectById(postId);
        // 检查用户是否存在且状态为正常
        if (user == null||user.getUserStatus()!=0 ) {
            return false;
        }
        // 检查帖子是否存在且状态为正常
        if (post == null||post.getPostStatus() !=1) {
            return false;
        }
        // 检查用户是否已收藏该帖子
        if (collect != null) {
            return false;
        }
        collect = new BBSPostCollect();
        collect.setUserId(userId);
        collect.setPostId(postId);
        collect.setCreateTime(new Date());
        post.setPostCollects(post.getPostCollects()+1);
        return collectMapper.insert(collect)>0 && postMapper.updateById(post)>0&& redisUtil.set("post:"+postId,post);
    }

    @Override
    @Transactional
    public Boolean deleteCollectRecord(Long userId, Long postId) {
        LambdaQueryWrapper<BBSPostCollect> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSPostCollect::getUserId, userId);
        Wrapper.eq(BBSPostCollect::getPostId, postId);
        BBSPostCollect collect = collectMapper.selectOne(Wrapper);
        BBSUser user = userMapper.selectById(userId);
        BBSPost post = postMapper.selectById(postId);
        // 检查用户是否存在且状态为正常
        if (user == null||user.getUserStatus() !=0) {
            return false;
        }
        // 检查帖子是否存在且状态为正常
        if (post == null||post.getPostStatus() !=1) {
            return false;
        }
        // 检查用户是否已收藏该帖子
        if (collect == null) {
            return false;
        }
      Long postCollects = post.getPostCollects();
        // 检查帖子收藏量是否为0
        if (postCollects > 0) {
            post.setPostCollects(postCollects-1);
        }
        return collectMapper.deleteById(collect)>0&& postMapper.updateById(post)>0&& redisUtil.set("post:"+postId,post);

    }

    @Override
    public Boolean validUserCollect(Long userId, Long postId) {
        LambdaQueryWrapper<BBSPostCollect> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSPostCollect::getUserId, userId);
        Wrapper.eq(BBSPostCollect::getPostId, postId);
        BBSPostCollect collect = collectMapper.selectOne(Wrapper);
        if (collect == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<BBSPost> getCollectRecordsByUserId(Long userId) {
        LambdaQueryWrapper<BBSPostCollect> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSPostCollect::getUserId, userId);
        List<BBSPostCollect> collectList = collectMapper.selectList(Wrapper);
        List<BBSPost> postList = collectList.stream().map(collect ->
                redisUtil.get("post:"+collect.getPostId())!=null? (BBSPost) redisUtil.get("post:"+collect.getPostId()): postMapper.selectById(collect.getPostId())).toList();
        return postList;
    }
}
