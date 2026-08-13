package com.bbs.bbs.handller.api;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.service.BBSPostService;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.post.CreatePostRequest;
import com.bbs.bbs.web.dto.post.PostPageRequest;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.dto.post.UpdatePostRequest;
import com.bbs.bbs.web.response.ApiResult;
import com.bbs.bbs.web.response.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostApiController {
    private final BBSPostService postService;
    private final BBSUserService userService;

    public PostApiController(BBSPostService postService, BBSUserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping
    public ApiResult<PageResult<PostResponse>> list(@Valid @ModelAttribute PostPageRequest request) {
        return ApiResult.success(postService.listPublicPosts(request));
    }

    @GetMapping("/{id}")
    public ApiResult<PostResponse> detail(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails principal) {
        Long userId = principal == null ? null : currentUser(principal).getUserId();
        boolean admin = principal != null && principal.getAuthorities().stream()
                .anyMatch(value -> "ROLE_ADMIN".equals(value.getAuthority()));
        return ApiResult.success(postService.getVisiblePost(id, userId, admin));
    }

    @PostMapping
    public ApiResult<PostResponse> create(@AuthenticationPrincipal UserDetails principal,
                                          @Valid @RequestBody CreatePostRequest request) {
        return ApiResult.success(postService.createPost(currentUser(principal).getUserId(), request));
    }

    @PutMapping("/{id}")
    public ApiResult<PostResponse> update(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails principal,
                                          @Valid @RequestBody UpdatePostRequest request) {
        return ApiResult.success(postService.updatePost(currentUser(principal).getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails principal) {
        postService.deletePost(currentUser(principal).getUserId(), id);
        return ApiResult.success(null);
    }

    private BBSUser currentUser(UserDetails principal) {
        return userService.findActiveByLoginName(principal.getUsername());
    }
}
