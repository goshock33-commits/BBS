package com.bbs.bbs.handller;

import com.bbs.bbs.bean.BBSPostComment;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.common.ServiceResultEnum;
import com.bbs.bbs.service.BBSCommentService;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author qq
 * @Description
 * @Date 2026-05-03 16:01:55
 * @Version 1.0
 */
@Controller
public class BBSCommentController {
    @Resource
    private BBSCommentService bbsPostCommentService;

    /**
     * 发表评论
     */
    @PostMapping("/replyPost")
    @ResponseBody
    public Result<String> replyPost(@RequestParam("postId") Long postId,
                            @RequestParam(value = "parentCommentUserId",
                                    required = false) Long parentCommentUserId,
                            @RequestParam("commentBody") String commentBody,
                            @RequestParam("verifyCode") String verifyCode,
                            HttpSession httpSession) {
        // 参数校验
        if (null == postId || postId < 0) {
            return ResultGenerator.genFailResult("postId参数错误");
        }
        if (!StringUtils.hasLength(commentBody)) {
            return ResultGenerator.genFailResult("commentBody参数错误");
        }
        if (commentBody.trim().length() > 200) {
            return ResultGenerator.genFailResult("评论内容过长");
        }
        // 验证码校验
        String kaptchaCode = httpSession.getAttribute(Constants.VERIFY_CODE_KEY) + "";
        if (!StringUtils.hasLength(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
            return ResultGenerator.genFailResult(
                    ServiceResultEnum.LOGIN_VERIFY_CODE_ERROR.getResult());
        }
        // 从 Session 获取当前登录用户
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        if (null == bbsUser) {
            return ResultGenerator.genFailResult("请先登录");
        }
        // 组装评论对象
        BBSPostComment bbsPostComment = new BBSPostComment();
        bbsPostComment.setCommentBody(commentBody);
        bbsPostComment.setCommentUserId(bbsUser.getUserId());
        bbsPostComment.setParentCommentUserId(parentCommentUserId);
        bbsPostComment.setPostId(postId);

        if (bbsPostCommentService.addPostComment(bbsPostComment)) {
            httpSession.removeAttribute(Constants.VERIFY_CODE_KEY);
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("请求失败，请检查参数及账号是否有操作权限");
        }
    }

    /**
     * 删除评论
     */
    @PostMapping("/delReply/{commentId}")
    @ResponseBody
    public Result delReply(@PathVariable("commentId") Long commentId,
                           HttpSession httpSession) {
        if (null == commentId || commentId < 0) {
            return ResultGenerator.genFailResult("commentId参数错误");
        }
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        if (bbsPostCommentService.delPostComment(commentId, bbsUser.getUserId())) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("请求失败，请检查参数及账号是否有操作权限");
        }
    }
}
