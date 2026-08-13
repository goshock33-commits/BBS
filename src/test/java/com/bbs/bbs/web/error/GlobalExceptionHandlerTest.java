package com.bbs.bbs.web.error;

import com.bbs.bbs.web.dto.auth.RegisterRequest;
import com.bbs.bbs.web.filter.TraceIdFilter;
import com.bbs.bbs.web.response.ApiResult;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@ContextConfiguration(classes = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @RestController
    static class TestController {
        @PostMapping("/test/register")
        ApiResult<Void> register(@Valid @RequestBody RegisterRequest request) {
            return ApiResult.success(null);
        }

        @GetMapping("/test/ownership")
        ApiResult<Void> ownership() {
            throw new BusinessException(ErrorCode.FORBIDDEN_OWNERSHIP, "无权操作该资源");
        }

        @GetMapping("/test/state")
        ApiResult<Void> state() {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "状态流转不合法");
        }

        @GetMapping("/test/category")
        ApiResult<Void> category() {
            throw new BusinessException(ErrorCode.CATEGORY_DISABLED, "分类已停用");
        }
    }

    @Test
    void invalidEmailReturnsStableErrorEnvelope() throws Exception {
        mvc.perform(post("/test/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginName":"bad","password":"123","nickName":"",
                                 "verifyCode":"abcd"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void ownershipFailureReturnsForbidden() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/test/ownership"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OWNERSHIP"));
    }

    @Test
    void invalidStateTransitionReturnsConflict() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/test/state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void disabledCategoryReturnsConflict() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/test/category"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_DISABLED"));
    }
}
