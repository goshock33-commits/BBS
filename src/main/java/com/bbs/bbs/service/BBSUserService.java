package com.bbs.bbs.service;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.web.dto.auth.RegisterRequest;
import com.bbs.bbs.web.dto.user.ChangePasswordRequest;
import com.bbs.bbs.web.dto.user.UpdateProfileRequest;
import jakarta.servlet.http.HttpSession;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 20:13:52
 * @Version 1.0
 */
public interface BBSUserService {
    void register(RegisterRequest request);

    BBSUser findActiveByLoginName(String loginName);

    void recordSuccessfulLogin(Long userId);

    void updateProfile(Long currentUserId, UpdateProfileRequest request);

    void changePassword(Long currentUserId, ChangePasswordRequest request);

    /**
     * 用户注册
     *
     * @param loginName 登录名（邮箱格式）
     * @param password  明文密码
     * @param nickName  昵称
     * @return 注册结果（ServiceResultEnum 枚举值）
     */
    String register(String loginName, String password, String nickName);

    /**
     * 用户登录（第05课实现）
     */
    String login(String loginName, String password, HttpSession httpSession);

    /**
     * 根据 ID 查询用户（第10课实现）
     */
    BBSUser getUserById(Long userId);

    /**
     * 修改用户信息（第10课实现）
     */
    Boolean updateUserInfo(BBSUser bbsUser, HttpSession httpSession);

    /**
     * 修改用户头像（第10课实现）
     */
    Boolean updateUserHeadImg(BBSUser bbsUser, HttpSession httpSession);

    /**
     * 修改密码（第10课实现）
     */
    Boolean updatePassword(Long userId, String originalPassword, String newPassword);

}
