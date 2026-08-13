package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbs.bbs.Mapper.BBSCategoryMapper;
import com.bbs.bbs.bean.BBSPostCategory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-26 20:47:13
 * @Version 1.0
 */
@Service
public class BBSPostCategoryServiceImp implements BBSPostCategoryService {
    private final BBSCategoryMapper baseMapper;

    public BBSPostCategoryServiceImp(BBSCategoryMapper baseMapper) {
        this.baseMapper = baseMapper;
    }

    public List<BBSPostCategory> getEnabledCategories() {
        LambdaQueryWrapper<BBSPostCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BBSPostCategory::getEnabled, true)
                .orderByDesc(BBSPostCategory::getCategoryRank);
        return baseMapper.selectList(queryWrapper).stream()
                .filter(category -> Boolean.TRUE.equals(category.getEnabled()))
                .toList();
    }

    public BBSPostCategory requireEnabledCategory(Integer categoryId) {
        BBSPostCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
        }
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new BusinessException(ErrorCode.CATEGORY_DISABLED, "分类已停用");
        }
        return category;
    }

    @Override
    public List<BBSPostCategory> getBBSPostCategories() {
        return getEnabledCategories();
    }

    @Override
    public BBSPostCategory selectByName(String categoryName) {
        LambdaQueryWrapper<BBSPostCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BBSPostCategory::getCategoryName, categoryName);
        return baseMapper.selectOne(queryWrapper);
    }
}
