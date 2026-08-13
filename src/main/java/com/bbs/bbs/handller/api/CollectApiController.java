package com.bbs.bbs.handller.api;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.service.BBSPostCollectService;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.collect.CollectResponse;
import com.bbs.bbs.web.response.ApiResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/collect")
public class CollectApiController {
    private final BBSPostCollectService collectService;
    private final BBSUserService userService;

    public CollectApiController(BBSPostCollectService collectService, BBSUserService userService) {
        this.collectService = collectService;
        this.userService = userService;
    }

    @PutMapping
    public ApiResult<CollectResponse> collect(@PathVariable Long postId,
                                              @AuthenticationPrincipal UserDetails principal) {
        BBSUser user = userService.findActiveByLoginName(principal.getUsername());
        return ApiResult.success(collectService.collect(user.getUserId(), postId));
    }

    @DeleteMapping
    public ApiResult<CollectResponse> cancel(@PathVariable Long postId,
                                             @AuthenticationPrincipal UserDetails principal) {
        BBSUser user = userService.findActiveByLoginName(principal.getUsername());
        return ApiResult.success(collectService.cancelCollect(user.getUserId(), postId));
    }
}
