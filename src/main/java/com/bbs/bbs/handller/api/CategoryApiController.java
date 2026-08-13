package com.bbs.bbs.handller.api;

import com.bbs.bbs.service.BBSPostCategoryService;
import com.bbs.bbs.web.dto.category.CategoryResponse;
import com.bbs.bbs.web.response.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryApiController {
    private final BBSPostCategoryService categoryService;

    public CategoryApiController(BBSPostCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResult<List<CategoryResponse>> list() {
        List<CategoryResponse> categories = categoryService.getBBSPostCategories().stream()
                .map(value -> new CategoryResponse(value.getCategoryId(), value.getCategoryName(),
                        value.getCategoryRank(), Boolean.TRUE.equals(value.getEnabled())))
                .toList();
        return ApiResult.success(categories);
    }
}
