package com.bbs.bbs.handller;

import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSPostCategory;
import com.bbs.bbs.service.BBSPostCategoryService;
import com.bbs.bbs.service.BBSPostService;
import com.bbs.util.PageQueryUtil;
import com.bbs.util.PageResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-20 11:40:38
 * @Version 1.0
 */
@Controller
public class indexController {
    @Resource
    private BBSPostCategoryService bbsPostCategoryService;
    @Resource
    private BBSPostService bbsPostService;

    @GetMapping({"", "/", "/index", "/index.html"})
    public String indexPage(
            HttpServletRequest request,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "orderBy", required = false) String orderBy) {

        // 1. 查询帖子分类（左侧导航栏）
        List<BBSPostCategory> bbsPostCategories =
                bbsPostCategoryService.getBBSPostCategories();
        if (CollectionUtils.isEmpty(bbsPostCategories)) {
            return "error/error_404";
        }
        request.setAttribute("bbsPostCategories", bbsPostCategories);

        // 2. 查询热议帖子（右侧边栏）
        request.setAttribute("hotTopicBBSPostList",
                bbsPostService.getHotTopicBBSPostList());

        // 3. 封装查询参数
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("limit", 10);  // 每页 10 条

        // 按分类筛选
        if (categoryId != null && categoryId > 0) {
            request.setAttribute("categoryId", categoryId);
            params.put("categoryId", categoryId);
        }
        // 关键字搜索
        if (StringUtils.hasLength(keyword)) {
            request.setAttribute("keyword", keyword);
            params.put("keyword", keyword);
        }
        // 时间周期（week/month/all）
        if (StringUtils.hasLength(period)) {
            request.setAttribute("period", period);
            params.put("period", period);
        }
        // 排序方式（views/time）
        if (StringUtils.hasLength(orderBy)) {
            request.setAttribute("orderBy", orderBy);
            params.put("orderBy", orderBy);
        }

        PageQueryUtil pageUtil = new PageQueryUtil(params);

        // 4. 查询帖子分页数据
        request.setAttribute("pageResult",
                bbsPostService.getBBSPostPageForIndex(pageUtil));
        return "index";
    }
}

