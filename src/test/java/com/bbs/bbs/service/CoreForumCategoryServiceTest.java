package com.bbs.bbs.service;

import com.bbs.bbs.Mapper.BBSCategoryMapper;
import com.bbs.bbs.bean.BBSPostCategory;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreForumCategoryServiceTest {

    @Mock
    BBSCategoryMapper categoryMapper;

    private BBSPostCategoryServiceImp service;

    @BeforeEach
    void setUp() {
        service = new BBSPostCategoryServiceImp(categoryMapper);
    }

    @Test
    void publicCategoriesExcludeDisabledRows() {
        BBSPostCategory enabled = category(1, true, 20);
        BBSPostCategory disabled = category(2, false, 30);
        when(categoryMapper.selectList(any())).thenReturn(List.of(disabled, enabled));

        List<BBSPostCategory> result = service.getEnabledCategories();

        assertEquals(List.of(enabled), result);
    }

    @Test
    void disabledCategoryCannotBeUsedForPublishing() {
        when(categoryMapper.selectById(2)).thenReturn(category(2, false, 10));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requireEnabledCategory(2));

        assertEquals(ErrorCode.CATEGORY_DISABLED, exception.getErrorCode());
    }

    private BBSPostCategory category(int id, boolean enabled, int rank) {
        BBSPostCategory category = new BBSPostCategory();
        category.setCategoryId(id);
        category.setCategoryName("分类" + id);
        category.setEnabled(enabled);
        category.setCategoryRank(rank);
        return category;
    }
}
