package com.bbs.bbs.handller;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.service.BBSPostCollectService;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author qq
 * @Description
 * @Date 2026-05-13 16:54:00
 * @Version 1.0
 */
@Controller
public class BBSPostCollectController {
    @Resource
    private BBSPostCollectService bbsPostCollectService;

    /**
     * 收藏帖子
     */
    @PostMapping("/addCollect/{postId}")
    @ResponseBody
    public Result<String> addCollect(@PathVariable("postId") Long postId,
                             HttpSession httpSession) {
        if (null == postId || postId < 0) {
            return ResultGenerator.genFailResult("postId参数错误");
        }
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        if (bbsPostCollectService.addCollectRecord(bbsUser.getUserId(), postId)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("请求失败，请检查参数及账号是否有操作权限");
        }
    }

    /**
     * 取消收藏
     */
    @PostMapping("/delCollect/{postId}")
    @ResponseBody
    public Result delCollect(@PathVariable("postId") Long postId,
                             HttpSession httpSession) {
        if (null == postId || postId < 0) {
            return ResultGenerator.genFailResult("postId参数错误");
        }
        BBSUser bbsUser = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
        if (bbsPostCollectService.deleteCollectRecord(bbsUser.getUserId(), postId)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("请求失败，请检查参数及账号是否有操作权限");
        }
    }
}
