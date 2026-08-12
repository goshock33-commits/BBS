package com.bbs.bbs.handller;

import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.RecentCommentListEntity;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.service.*;
import com.bbs.bbs.web.dto.user.ChangePasswordRequest;
import com.bbs.bbs.web.dto.user.UpdateProfileRequest;
import com.bbs.bbs.web.response.ApiResult;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-06-06 22:53:36
 * @Version 1.0
 */
@Controller
public class BBSUserController {
    @Resource
    private BBSPostService bbsPostService;
    @Resource
    private BBSUserService bbsUserService;

    @Resource
    private BBSCommentService bbsPostCommentService;
    @Resource
    private BBSPostCollectService bbsPostCollectService;
    @GetMapping("/userCenter/{userId}")
    public String userCenterPage(HttpServletRequest request,
                                 @PathVariable("userId") Long userId) {
        // 基本用户信息
        BBSUser bbsUser = bbsUserService.getUserById(userId);
        if (bbsUser == null) {
            return "error/error_404";
        }
        // 近期发布的帖子
        List<BBSPost> recentPostList = bbsPostService.getRecentPostListByUserId(userId);
        // 近期回复的内容
        List<RecentCommentListEntity> recentCommentList =
                bbsPostCommentService.getRecentCommentListByUserId(userId);

        request.setAttribute("bbsUser", bbsUser);
        request.setAttribute("recentPostList", recentPostList);
        request.setAttribute("recentCommentList", recentCommentList);
        return "user/home";
    }
    @GetMapping("/myCenter")
    public String myCenterPage(HttpServletRequest request) {
        BBSUser currentUser = (BBSUser) request.getSession()
                .getAttribute(Constants.USER_SESSION_KEY);

        // 我发的帖子
        List<BBSPost> myBBSPostList =
                bbsPostService.getMyBBSPostList(currentUser.getUserId());
        int myBBSPostCount = 0;
        if (!CollectionUtils.isEmpty(myBBSPostList)) {
            myBBSPostCount = myBBSPostList.size();
        }

        // 我收藏的帖子
        List<BBSPost> collectRecords =
                bbsPostCollectService.getCollectRecordsByUserId(currentUser.getUserId());
        int myCollectBBSPostCount = 0;
        if (!CollectionUtils.isEmpty(collectRecords)) {
            myCollectBBSPostCount = collectRecords.size();
        }

        request.setAttribute("myBBSPostList", myBBSPostList);
        request.setAttribute("myBBSPostCount", myBBSPostCount);
        request.setAttribute("collectRecords", collectRecords);
        request.setAttribute("myCollectBBSPostCount", myCollectBBSPostCount);
        request.setAttribute("bbsUser", currentUser);
        return "user/index";
    }
    @GetMapping("/userSet")
    public String userSetPage(HttpServletRequest request) {
        BBSUser currentUser = (BBSUser) request.getSession()
                .getAttribute(Constants.USER_SESSION_KEY);
        request.setAttribute("bbsUser", currentUser);
        return "user/set";
    }
    @PutMapping("/api/v1/me/profile")
    @ResponseBody
    public ApiResult<Void> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpSession session) {
        BBSUser currentUser = bbsUserService.findActiveByLoginName(principal.getUsername());
        bbsUserService.updateProfile(currentUser.getUserId(), request);
        BBSUser refreshedUser = bbsUserService.findActiveByLoginName(principal.getUsername());
        session.setAttribute(Constants.USER_SESSION_KEY, refreshedUser);
        return ApiResult.success(null);
    }
    @PostMapping("/updateHeadImg")
    @ResponseBody
    public Result<String> updateHeadImg(@RequestParam("userHeadImg") String userHeadImg,
                                HttpSession httpSession) {
        if (!StringUtils.hasLength(userHeadImg)) {
            return ResultGenerator.genFailResult("userHeadImg参数错误");
        }
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        bbsUser.setHeadImgUrl(userHeadImg);
        if (bbsUserService.updateUserHeadImg(bbsUser, httpSession)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("修改失败");
        }
    }
    @PutMapping("/api/v1/me/password")
    @ResponseBody
    public ApiResult<Void> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest) {
        BBSUser currentUser = bbsUserService.findActiveByLoginName(principal.getUsername());
        bbsUserService.changePassword(currentUser.getUserId(), request);
        SecurityContextHolder.clearContext();
        servletRequest.getSession(false).invalidate();
        return ApiResult.success(null);
    }
}
