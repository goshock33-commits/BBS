package com.bbs.bbs.handller;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.bbs.bbs.bean.BBSPost;
import com.bbs.bbs.bean.BBSPostCategory;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.common.ServiceResultEnum;
import com.bbs.bbs.service.BBSCommentService;
import com.bbs.bbs.service.BBSPostCategoryService;
import com.bbs.bbs.service.BBSPostCollectService;
import com.bbs.bbs.service.BBSPostService;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-30 11:36:25
 * @Version 1.0
 */
@Controller
public class BBSPostController {
    @Resource
    private BBSPostService bbsPostService;
    @Resource
    private BBSPostCategoryService bbsPostCategoryService;
    @Resource
    private BBSCommentService bbsCommentService;
    @Resource
    private BBSPostCollectService bbsPostCollectService;

    @GetMapping("addPostPage")
    public String addPostPage(ModelMap modelMap) {
        List<BBSPostCategory> bbsPostCategories =
                bbsPostCategoryService.getBBSPostCategories();
        if (CollectionUtils.isEmpty(bbsPostCategories)) {
            return "error/error_404";
        }
        // 将分类列表传到页面，供下拉框使用
        modelMap.put("bbsPostCategories", bbsPostCategories);
        return "jie/add";
    }

    @PostMapping("/addPost")
    @ResponseBody
    public Result<String> addPost(@RequestParam("postTitle") String postTitle,
                                  @RequestParam("postCategoryName") String postCategoryName,
                                  @RequestParam("postContent") String postContent,
                                  @RequestParam("verifyCode") String verifyCode, HttpSession httpSession) {
        // 从会话中获取当前登录用户
        BBSUser currentUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        if (currentUser == null) {
            return ResultGenerator.genFailResult("请先登录");
        }
        // 调用服务层添加帖子
        if (!StringUtils.hasLength(postTitle)) {
            return ResultGenerator.genFailResult("标题不能为空");
        }
        if (postTitle.trim().length() > 32) {
            return ResultGenerator.genFailResult("标题过长，最多32个字符");
        }
        if (!StringUtils.hasLength(postCategoryName)) {
            return ResultGenerator.genFailResult("请选择帖子分类");
        }
        // 校验分类是否存在
        BBSPostCategory bbsPostCategory =
                bbsPostCategoryService.selectByName(postCategoryName);
        if (bbsPostCategory == null) {
            return ResultGenerator.genFailResult("分类不存在");
        }
        if (!StringUtils.hasLength(postContent)) {
            return ResultGenerator.genFailResult("内容不能为空");
        }
        if (postContent.trim().length() > 100000) {
            return ResultGenerator.genFailResult("内容过长");
        }
        // ===== 验证码校验 =====
        String kaptchaCode = httpSession.getAttribute(
                Constants.VERIFY_CODE_KEY) + "";
        if (!StringUtils.hasLength(kaptchaCode)
                || !verifyCode.equals(kaptchaCode)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_VERIFY_CODE_ERROR.getResult());
        }

        // ===== 构建帖子对象并保存 =====
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(
                Constants.USER_SESSION_KEY);
        BBSPost bbsPost = new BBSPost();
        bbsPost.setPublishUserId(bbsUser.getUserId());
        bbsPost.setPostTitle(postTitle);
        bbsPost.setPostContent(postContent);
        bbsPost.setPostCategoryName(postCategoryName);
        bbsPost.setPostCategoryId(bbsPostCategory.getCategoryId());

        if (bbsPostService.addPost(bbsPost, currentUser) == 1) {
            // 发帖成功，清除验证码
            httpSession.removeAttribute(Constants.VERIFY_CODE_KEY);
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("发帖失败，请稍后重试");
        }
    }

    /**
     * 帖子详情页
     */
    @GetMapping("/detail/{postId}")
    public String postDetail(ModelMap modelMap,
                             @PathVariable("postId") Long postId,
                             @RequestParam(value = "commentPage", defaultValue = "1") int commentPage, HttpSession httpSession) {
        // 1. 查询分类列表（页面顶部导航用）
        List<BBSPostCategory> bbsPostCategories =
                bbsPostCategoryService.getBBSPostCategories();
        // 从会话中获取当前登录用户
        BBSUser loginUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        if (loginUser == null) {
            modelMap.addAttribute("message", "请先登录");
            return "redirect:/login";
        }
        modelMap.addAttribute("loginUser", loginUser);
        if (CollectionUtils.isEmpty(bbsPostCategories)) {
            return "error/error_404";
        }
        modelMap.addAttribute("bbsPostCategories", bbsPostCategories);

        // 2. 查询帖子详情（浏览量自动+1）
        BBSPost bbsPost = bbsPostService.getBBSPostForDetail(postId, loginUser.getUserId());
        if (bbsPost == null) {
            return "error/error_404";
        }
        modelMap.addAttribute("bbsPost", bbsPost);

        // 3. 查询发帖人信息
        BBSUser postAuthor = bbsPost.getUser();
        if (postAuthor == null) {
            return "error/error_404";
        }
        modelMap.addAttribute("bbsUser", postAuthor);

        // 4. 查询本周热议帖子
        modelMap.addAttribute("hotTopicBBSPostList",
                bbsPostService.getHotTopicBBSPostList());

        // 5. 查询评论分页数据
        modelMap.addAttribute("commentsPage",
                bbsCommentService.getCommentsByPostId(postId, commentPage));
// 当前登录用户是否收藏了本帖
        BBSUser currentUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        modelMap.addAttribute("currentUserCollectFlag",
                bbsPostCollectService.validUserCollect(currentUser.getUserId(), postId));
        return "jie/detail";
    }

    /**
     * 编辑帖子页面
     */
    @GetMapping("/editPostPage/{postId}")

    public String editPostPage(ModelMap modelMap,
                               @PathVariable("postId") Long postId, HttpSession httpSession) {
        // 获取当前登录用户
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        if (bbsUser == null) {
            modelMap.addAttribute("message", "请先登录");
            return "error/error_404";
        }
        // 查询分类列表
        List<BBSPostCategory> bbsPostCategories =
                bbsPostCategoryService.getBBSPostCategories();
        if (CollectionUtils.isEmpty(bbsPostCategories)) {
            modelMap.addAttribute("message", "分类不存在");
            return "error/error_404";
        }
        modelMap.addAttribute("bbsPostCategories", bbsPostCategories);

        // 校验帖子是否存在
        if (postId == null || postId < 0) {
            modelMap.addAttribute("message", "帖子不存在");
            return "error/error_404";
        }
        BBSPost bbsPost = bbsPostService.getBBSPostById(postId);
        if (bbsPost == null) {
            return "error/error_404";
        }

        // 校验是否本人发帖
        if (!bbsUser.getUserId().equals(bbsPost.getPublishUserId())) {
            modelMap.addAttribute("message", "非本人发帖，无权限修改");
            return "error/error";
        }

        modelMap.addAttribute("bbsPost", bbsPost);
        modelMap.addAttribute("postId", postId);
        return "jie/edit";
    }

    /**
     * 提交编辑帖子
     */
    @PostMapping("/editPost")
    @ResponseBody
    public Result<String> editPost(@RequestParam("postId") Long postId,
                                   @RequestParam("postTitle") String postTitle,
                                   @RequestParam("postCategoryName") String postCategoryName,
                                   @RequestParam("postContent") String postContent,
                                   @RequestParam("verifyCode") String verifyCode,
                                   ModelMap modelMap, HttpSession httpSession) {
        // 获取当前登录用户
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(
                Constants.USER_SESSION_KEY);
        if (bbsUser == null) {
            return ResultGenerator.genFailResult("请先登录");
        }
        // ===== 帖子存在性校验 =====
        if (postId == null || postId < 0) {
            return ResultGenerator.genFailResult("postId参数错误");
        }
        BBSPost existPost = bbsPostService.getBBSPostById(postId);
        if (existPost == null) {
            return ResultGenerator.genFailResult("帖子不存在");
        }

        // ===== 权限校验：只能编辑自己的帖子 =====
        if (!bbsUser.getUserId().equals(existPost.getPublishUserId())) {
            return ResultGenerator.genFailResult("非本人发帖，无权限修改");
        }

        // ===== 参数校验 =====
        if (!StringUtils.hasLength(postTitle)) {
            return ResultGenerator.genFailResult("标题不能为空");
        }
        if (postTitle.trim().length() > 32) {
            return ResultGenerator.genFailResult("标题过长，最多32个字符");
        }
        if (postCategoryName == null) {
            return ResultGenerator.genFailResult("请选择帖子分类");
        }
        BBSPostCategory bbsPostCategory =
                bbsPostCategoryService.selectByName(postCategoryName);
        if (bbsPostCategory == null) {
            return ResultGenerator.genFailResult("分类不存在");
        }
        if (!StringUtils.hasLength(postContent)) {
            return ResultGenerator.genFailResult("内容不能为空");
        }
        if (postContent.trim().length() > 100000) {
            return ResultGenerator.genFailResult("内容过长");
        }

        // ===== 验证码校验 =====
        String kaptchaCode =httpSession.getAttribute(
                Constants.VERIFY_CODE_KEY) + "";
        if (!StringUtils.hasLength(kaptchaCode)
                || !verifyCode.equals(kaptchaCode)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_VERIFY_CODE_ERROR.getResult());
        }

        // ===== 更新帖子 =====
        existPost.setPostTitle(postTitle);
        existPost.setPostContent(postContent);
        existPost.setPostCategoryName(postCategoryName);
        existPost.setPostCategoryId(bbsPostCategory.getCategoryId());
        existPost.setLastUpdateTime(new Date());

        if (bbsPostService.updateBBSPost(existPost) > 0) {
            httpSession.removeAttribute(Constants.VERIFY_CODE_KEY);
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("修改失败，请稍后重试");
        }
    }

    /**
     * 删除帖子
     */
    @PostMapping("/delPost/{postId}")
    @ResponseBody
    public Result<String> delPost(@PathVariable("postId") Long postId,
                                  ModelMap modelMap, HttpSession httpSession) {
        if (postId == null || postId < 0) {
            return ResultGenerator.genFailResult("postId参数错误");
        }
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(
                Constants.USER_SESSION_KEY);
        if (bbsUser == null) {
            return ResultGenerator.genFailResult("请先登录");
        }
        if (bbsPostService.delBBSPost(bbsUser.getUserId(), postId) > 0) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult(
                    "删除失败，请确认是否为本人发帖");
        }
    }
}

