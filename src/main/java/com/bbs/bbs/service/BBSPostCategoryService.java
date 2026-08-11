package com.bbs.bbs.service;

import com.bbs.bbs.bean.BBSPostCategory;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-26 20:46:06
 * @Version 1.0
 */
public interface BBSPostCategoryService {
    /**
     * 获取所有未删除的分类，按 categoryRank 降序排列
     */
    List<BBSPostCategory> getBBSPostCategories();

    /**
     * 按 ID 查询分类
     */
    BBSPostCategory selectByName(String categoryName);
}
