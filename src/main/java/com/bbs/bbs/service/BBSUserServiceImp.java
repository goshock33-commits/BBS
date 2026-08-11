package com.bbs.bbs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.bean.RedisUtil;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.common.ServiceResultEnum;
import com.bbs.util.SHA256Util;
import com.bbs.util.SystemUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-21 20:30:21
 * @Version 1.0
 */
@Service
public class BBSUserServiceImp implements BBSUserService {
    @Autowired
    private BBSUserMapper bbsUserMapper;
    @Resource
    private RedisUtil redisUtil;
    @Override
    public String register(String loginName, String password, String nickName) {
        // 检查登录名是否已存在
        LambdaQueryWrapper<BBSUser> Wrapper = new LambdaQueryWrapper<>();
        Wrapper.eq(BBSUser::getLoginName, loginName);
        BBSUser user = bbsUserMapper.selectOne(Wrapper);
        if (user != null) {
            return ServiceResultEnum.SAME_LOGIN_NAME_EXIST.getResult();
        }
        // 不存在 注册
        else {
            try{
                BBSUser newUser = new BBSUser();
                newUser.setLoginName(loginName);
                newUser.setNickName(nickName);
                newUser.setHeadImgUrl("/images/avatar/default.png"); // 默认头像
                newUser.setIntroduce("这个人很懒，什么都没留下~");     // 默认简介
                newUser.setLocation("未知");
                newUser.setGender("未知");
                String passWord = SHA256Util.SHA256Encode(password,"UTF-8");
                newUser.setPasswordMd5(passWord);
                if(bbsUserMapper.insert(newUser) > 0){
                    return ServiceResultEnum.REGISTER_SUCCESS.getResult();
                }
                else{
                    return ServiceResultEnum.REGISTER_ERROR.getResult();
                }
            }
                catch(Exception e){
                    return ServiceResultEnum.OTHER.getResult()+"原因："+e.getMessage();
                }

        }

    }
    @Override
    public String login(String loginName, String passwordMD5, HttpSession httpSession) {
        LambdaQueryWrapper<BBSUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BBSUser::getLoginName, loginName).eq(BBSUser::getPasswordMd5, passwordMD5);
        BBSUser user = bbsUserMapper.selectOne(wrapper);
        if(user != null&&httpSession!=null){
            httpSession.setAttribute(Constants.USER_SESSION_KEY,user);
            //更新用户登录时间
            user.setLastLoginTime(new Date());
            bbsUserMapper.updateById(user);
            return ServiceResultEnum.LOGIN_SUCCESS.getResult();
        }
        return ServiceResultEnum.LOGIN_ERROR.getResult();
    }

    @Override
    public BBSUser getUserById(Long userId) {
        BBSUser user=(BBSUser)redisUtil.get("user:"+userId);
        if(user == null){
            user = bbsUserMapper.selectById(userId);
            redisUtil.set("user:"+userId,user);
        }
        return user;
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
                if (redisUtil.set("user:"+userFromDB.getUserId(),userFromDB)&&bbsUserMapper.updateById(userFromDB) > 0) {
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
            if (redisUtil.set("user:"+userFromDB.getUserId(),userFromDB)&&bbsUserMapper.updateById(userFromDB) > 0) {
                httpSession.setAttribute(Constants.USER_SESSION_KEY, userFromDB);
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean updatePassword(Long userId, String originalPassword, String newPassword) {
        BBSUser bbsUser = bbsUserMapper.selectById(userId);
        if (bbsUser != null && bbsUser.getUserStatus().intValue() == 0) {
            String originalPasswordMd5 = SHA256Util.SHA256Encode(originalPassword, "UTF-8");
            String newPasswordMd5 = SHA256Util.SHA256Encode(newPassword, "UTF-8");
            // 比较原密码是否正确
            if (originalPasswordMd5.equals(bbsUser.getPasswordMd5())) {
                bbsUser.setPasswordMd5(newPasswordMd5);
                if (redisUtil.set("user:"+bbsUser.getUserId(),bbsUser)&&bbsUserMapper.updateById(bbsUser) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
