package com.bbs.bbs.handller.api;

import com.bbs.bbs.service.AdminModerationService;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.admin.AdminPostFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminCommentFilterRequest;
import com.bbs.bbs.web.dto.admin.AdminSummaryResponse;
import com.bbs.bbs.web.dto.admin.ModerationRequest;
import com.bbs.bbs.web.dto.admin.UserStatusRequest;
import com.bbs.bbs.web.dto.category.CategoryRequest;
import com.bbs.bbs.web.dto.category.CategoryResponse;
import com.bbs.bbs.web.dto.comment.CommentResponse;
import com.bbs.bbs.web.dto.post.PostResponse;
import com.bbs.bbs.web.response.ApiResult;
import com.bbs.bbs.web.response.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {
    private final AdminModerationService moderationService;
    private final BBSUserService userService;

    public AdminApiController(AdminModerationService moderationService,
                              BBSUserService userService) {
        this.moderationService = moderationService;
        this.userService = userService;
    }

    @GetMapping("/posts")
    public ApiResult<PageResult<PostResponse>> listPosts(
            @Valid @ModelAttribute AdminPostFilterRequest request) {
        return ApiResult.success(moderationService.listPosts(request));
    }

    @PostMapping("/posts/{id}/hide")
    public ApiResult<Void> hidePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable("id") Long postId,
            @Valid @RequestBody ModerationRequest request) {
        Long operatorId = userService.findActiveByLoginName(
                principal.getUsername()).getUserId();
        moderationService.hidePost(operatorId, postId, request.reason().trim());
        return ApiResult.success(null);
    }

    @GetMapping("/summary")
    public ApiResult<AdminSummaryResponse> summary() {
        return ApiResult.success(moderationService.summary());
    }

    @GetMapping("/comments")
    public ApiResult<PageResult<CommentResponse>> listComments(
            @Valid @ModelAttribute AdminCommentFilterRequest request) {
        return ApiResult.success(moderationService.listComments(request));
    }

    @PostMapping("/posts/{id}/restore")
    public ApiResult<Void> restorePost(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody ModerationRequest request) {
        moderationService.restorePost(operatorId(principal), id, request.reason().trim());
        return ApiResult.success(null);
    }

    @PostMapping("/posts/{id}/delete")
    public ApiResult<Void> deletePost(@AuthenticationPrincipal UserDetails principal,
                                      @PathVariable("id") Long id,
                                      @Valid @RequestBody ModerationRequest request) {
        moderationService.deletePost(operatorId(principal), id, request.reason().trim());
        return ApiResult.success(null);
    }

    @PostMapping("/comments/{id}/hide")
    public ApiResult<Void> hideComment(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody ModerationRequest request) {
        moderationService.hideComment(operatorId(principal), id, request.reason().trim());
        return ApiResult.success(null);
    }

    @PostMapping("/comments/{id}/restore")
    public ApiResult<Void> restoreComment(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable("id") Long id,
                                          @Valid @RequestBody ModerationRequest request) {
        moderationService.restoreComment(operatorId(principal), id, request.reason().trim());
        return ApiResult.success(null);
    }

    @PostMapping("/comments/{id}/delete")
    public ApiResult<Void> deleteComment(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable("id") Long id,
                                         @Valid @RequestBody ModerationRequest request) {
        moderationService.deleteComment(operatorId(principal), id, request.reason().trim());
        return ApiResult.success(null);
    }

    @PostMapping("/users/{id}/disable")
    public ApiResult<Void> disableUser(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody UserStatusRequest request) {
        moderationService.disableUser(operatorId(principal), id, request.reason().trim());
        return ApiResult.success(null);
    }

    @PostMapping("/users/{id}/restore")
    public ApiResult<Void> restoreUser(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody UserStatusRequest request) {
        moderationService.restoreUser(operatorId(principal), id, request.reason().trim());
        return ApiResult.success(null);
    }

    @GetMapping("/categories")
    public ApiResult<List<CategoryResponse>> listCategories() {
        return ApiResult.success(moderationService.listCategories());
    }

    @PostMapping("/categories")
    public ApiResult<CategoryResponse> createCategory(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResult.success(moderationService.createCategory(
                operatorId(principal), request));
    }

    @PutMapping("/categories/{id}")
    public ApiResult<CategoryResponse> updateCategory(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable("id") Integer id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResult.success(moderationService.updateCategory(
                operatorId(principal), id, request));
    }

    @PostMapping("/categories/{id}/enable")
    public ApiResult<Void> enableCategory(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable("id") Integer id) {
        moderationService.setCategoryEnabled(operatorId(principal), id, true);
        return ApiResult.success(null);
    }

    @PostMapping("/categories/{id}/disable")
    public ApiResult<Void> disableCategory(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable("id") Integer id) {
        moderationService.setCategoryEnabled(operatorId(principal), id, false);
        return ApiResult.success(null);
    }

    private Long operatorId(UserDetails principal) {
        return userService.findActiveByLoginName(principal.getUsername()).getUserId();
    }
}
