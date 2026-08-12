package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.common.ServiceResultEnum;
import com.bbs.util.SystemUtil;
import com.bbs.bbs.web.dto.auth.RegisterRequest;
import com.bbs.bbs.web.dto.user.ChangePasswordRequest;
import com.bbs.bbs.web.dto.user.UpdateProfileRequest;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.nio.charset.StandardCharsets;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 20:30:21
 * @Version 1.0
 */
@Service
public class BBSUserServiceImp implements BBSUserService {
    private final BBSUserMapper bbsUserMapper;
    private final PasswordEncoder passwordEncoder;

    public BBSUserServiceImp(BBSUserMapper bbsUserMapper,
                             PasswordEncoder passwordEncoder) {
        this.bbsUserMapper = bbsUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void register(RegisterRequest request) {
        validateBcryptPassword(request.password());
        String normalizedLoginName = normalizeLoginName(request.loginName());
        LambdaQueryWrapper<BBSUser> query = new LambdaQueryWrapper<>();
        query.eq(BBSUser::getLoginName, normalizedLoginName);
        if (bbsUserMapper.selectCount(query) > 0) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_LOGIN_NAME, "该邮箱已注册");
        }

        BBSUser user = new BBSUser();
        user.setLoginName(normalizedLoginName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickName(request.nickName().trim());
        user.setHeadImgUrl("/images/avatar/default.png");
        user.setGender("未知");
        user.setLocation("未知");
        user.setIntroduce("");
        user.setRole("USER");
        user.setUserStatus((byte) 0);
        user.setCreateTime(new Date());
        if (bbsUserMapper.insert(user) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "注册失败");
        }
    }

    @Override
    public BBSUser findActiveByLoginName(String loginName) {
        LambdaQueryWrapper<BBSUser> query = new LambdaQueryWrapper<>();
        query.eq(BBSUser::getLoginName, normalizeLoginName(loginName));
        BBSUser user = bbsUserMapper.selectOne(query);
        if (user == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
        if (user.getUserStatus() == null || user.getUserStatus() != 0) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        return user;
    }

    @Override
    public void recordSuccessfulLogin(Long userId) {
        BBSUser patch = new BBSUser();
        patch.setUserId(userId);
        patch.setLastLoginTime(new Date());
        bbsUserMapper.updateById(patch);
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        BBSUser user = requireActiveUser(userId);
        user.setNickName(request.nickName().trim());
        user.setGender(request.gender());
        user.setLocation(request.location().trim());
        user.setIntroduce(request.introduce() == null
                ? "" : request.introduce().trim());
        requireSuccessfulUpdate(bbsUserMapper.updateById(user), "个人资料修改失败");
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        BBSUser user = requireActiveUser(userId);
        if (!passwordEncoder.matches(
                request.originalPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS, "原密码错误");
        }
        validateBcryptPassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        requireSuccessfulUpdate(bbsUserMapper.updateById(user), "密码修改失败");
    }

    @Override
    public String register(String loginName, String password, String nickName) {
        try {
            register(new RegisterRequest(loginName, password, nickName, "compat"));
            return ServiceResultEnum.REGISTER_SUCCESS.getResult();
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.DUPLICATE_LOGIN_NAME) {
                return ServiceResultEnum.SAME_LOGIN_NAME_EXIST.getResult();
            }
            return ServiceResultEnum.REGISTER_ERROR.getResult();
        }
    }

    @Override
    public String login(String loginName, String password, HttpSession httpSession) {
        try {
            BBSUser user = findActiveByLoginName(loginName);
            if (httpSession == null
                    || !passwordEncoder.matches(password, user.getPasswordHash())) {
                return ServiceResultEnum.LOGIN_ERROR.getResult();
            }
            httpSession.setAttribute(Constants.USER_SESSION_KEY, user);
            recordSuccessfulLogin(user.getUserId());
            return ServiceResultEnum.LOGIN_SUCCESS.getResult();
        } catch (BusinessException exception) {
            return ServiceResultEnum.LOGIN_ERROR.getResult();
        }
    }

    @Override
    public BBSUser getUserById(Long userId) {
        return bbsUserMapper.selectById(userId);
    }



    @Override
    public Boolean updateUserInfo(BBSUser bbsUser, HttpSession httpSession) {
            BBSUser userTemp = (BBSUser) httpSession.getAttribute(Constants.USER_SESSION_KEY);
            BBSUser userFromDB = bbsUserMapper.selectById(userTemp.getUserId());
            // 当前用户非空且状态正常才可以进行更改
            if (userFromDB != null && userFromDB.getUserStatus().intValue() == 0) {
                userFromDB.setIntroduce(SystemUtil.cleanString(bbsUser.getIntroduce()));
                userFromDB.setLocation(SystemUtil.cleanString(bbsUser.getLocation()));
                userFromDB.setGender(SystemUtil.cleanString(bbsUser.getGender()));
                userFromDB.setNickName(SystemUtil.cleanString(bbsUser.getNickName()));
                if (bbsUserMapper.updateById(userFromDB) > 0) {
                    // 更新成功后同步更新 Session 中的用户信息
                    httpSession.setAttribute(Constants.USER_SESSION_KEY, userFromDB);
                    return true;
                }
            }
            return false;
    }

    @Override
    public Boolean updateUserHeadImg(BBSUser bbsUser, HttpSession httpSession) {
        BBSUser userFromDB = bbsUserMapper.selectById(bbsUser.getUserId());
        if (userFromDB != null && userFromDB.getUserStatus().intValue() == 0) {
            userFromDB.setHeadImgUrl(bbsUser.getHeadImgUrl());
            if (bbsUserMapper.updateById(userFromDB) > 0) {
                httpSession.setAttribute(Constants.USER_SESSION_KEY, userFromDB);
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean updatePassword(Long userId, String originalPassword, String newPassword) {
        try {
            changePassword(userId,
                    new ChangePasswordRequest(originalPassword, newPassword));
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }

    private BBSUser requireActiveUser(Long userId) {
        BBSUser user = bbsUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        if (user.getUserStatus() == null || user.getUserStatus() != 0) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        return user;
    }

    private String normalizeLoginName(String loginName) {
        return loginName.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private void validateBcryptPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "密码不能超过 72 个 UTF-8 字节");
        }
    }

    private void requireSuccessfulUpdate(int updatedRows, String message) {
        if (updatedRows <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }
}
