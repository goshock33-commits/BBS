package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbs.bbs.Mapper.BBSCategoryMapper;
import com.bbs.bbs.bean.BBSPostCategory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-26 20:47:13
 * @Version 1.0
 */
@Service
public class BBSPostCategoryServiceImp implements BBSPostCategoryService {
    @Resource
    private BBSCategoryMapper baseMapper;

    @Override
    public List<BBSPostCategory> getBBSPostCategories() {
        LambdaQueryWrapper<BBSPostCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(BBSPostCategory::getCategoryRank);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public BBSPostCategory selectByName(String categoryName) {
        LambdaQueryWrapper<BBSPostCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BBSPostCategory::getCategoryName, categoryName);
        return baseMapper.selectOne(queryWrapper);
    }
}
