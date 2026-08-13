package com.bbs.bbs.handller.api;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.service.BBSCommentService;
import com.bbs.bbs.service.BBSUserService;
import com.bbs.bbs.web.dto.comment.CommentPageRequest;
import com.bbs.bbs.web.dto.comment.CommentResponse;
import com.bbs.bbs.web.dto.comment.CreateCommentRequest;
import com.bbs.bbs.web.response.ApiResult;
import com.bbs.bbs.web.response.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentApiController {
    private final BBSCommentService commentService;
    private final BBSUserService userService;

    public CommentApiController(BBSCommentService commentService, BBSUserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping("/api/v1/posts/{postId}/comments")
    public ApiResult<PageResult<CommentResponse>> list(@PathVariable Long postId,
                                                       @Valid @ModelAttribute CommentPageRequest request) {
        return ApiResult.success(commentService.listComments(postId, request));
    }

    @PostMapping("/api/v1/posts/{postId}/comments")
    public ApiResult<CommentResponse> create(@PathVariable Long postId,
                                             @AuthenticationPrincipal UserDetails principal,
                                             @Valid @RequestBody CreateCommentRequest request) {
        BBSUser user = userService.findActiveByLoginName(principal.getUsername());
        return ApiResult.success(commentService.createComment(user.getUserId(), postId, request));
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ApiResult<Void> delete(@PathVariable Long commentId,
                                  @AuthenticationPrincipal UserDetails principal) {
        BBSUser user = userService.findActiveByLoginName(principal.getUsername());
        commentService.deleteComment(user.getUserId(), commentId);
        return ApiResult.success(null);
    }
}
