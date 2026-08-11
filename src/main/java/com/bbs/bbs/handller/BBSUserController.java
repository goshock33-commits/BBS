package com.bbs.bbs.handller;

import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.RecentCommentListEntity;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.service.*;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    @PostMapping("/updateUserInfo")
    @ResponseBody
    public Result<String> updateInfo(@RequestParam("nickName") String nickName,
                             @RequestParam("userGender") int userGender,
                             @RequestParam("location") String location,
                             @RequestParam("introduce") String introduce,
                             HttpSession httpSession) {
        if (!StringUtils.hasLength(nickName)) {
            return ResultGenerator.genFailResult("nickName参数错误");
        }
        if (userGender < 1 || userGender > 2) {
            return ResultGenerator.genFailResult("userGender参数错误");
        }
        if (!StringUtils.hasLength(location)) {
            return ResultGenerator.genFailResult("location参数错误");
        }
        if (!StringUtils.hasLength(introduce)) {
            return ResultGenerator.genFailResult("introduce参数错误");
        }
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        bbsUser.setNickName(nickName);
        bbsUser.setGender(userGender == 1 ? "男" : "女");
        bbsUser.setLocation(location);
        bbsUser.setIntroduce(introduce);
        if (bbsUserService.updateUserInfo(bbsUser, httpSession)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("修改失败");
        }
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
    @PostMapping("/updatePassword")
    @ResponseBody
    public Result<String> passwordUpdate(HttpServletRequest request,
                                 @RequestParam("originalPassword") String originalPassword,
                                 @RequestParam("newPassword") String newPassword) {
        if (!StringUtils.hasLength(originalPassword)
                || !StringUtils.hasLength(newPassword)) {
            return ResultGenerator.genFailResult("参数不能为空");
        }
        BBSUser currentUser = (BBSUser) request.getSession()
                .getAttribute(Constants.USER_SESSION_KEY);
        if (bbsUserService.updatePassword(currentUser.getUserId(),
                originalPassword, newPassword)) {
            // 修改成功后清空 Session，前端跳转到登录页
            request.getSession().removeAttribute(Constants.USER_SESSION_KEY);
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("修改失败");
        }
    }
}
