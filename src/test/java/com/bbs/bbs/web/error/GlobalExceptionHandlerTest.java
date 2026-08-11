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
}
