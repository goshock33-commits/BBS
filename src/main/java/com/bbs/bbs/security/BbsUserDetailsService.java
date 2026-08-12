package com.bbs.bbs.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class BbsUserDetailsService implements UserDetailsService {
    private final BBSUserMapper userMapper;

    public BbsUserDetailsService(BBSUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        LambdaQueryWrapper<BBSUser> query = new LambdaQueryWrapper<>();
        query.eq(BBSUser::getLoginName, normalizedUsername);
        BBSUser user = userMapper.selectOne(query);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        String role = user.getRole() == null || user.getRole().isBlank()
                ? "USER" : user.getRole();
        return User.withUsername(user.getLoginName())
                .password(user.getPasswordHash())
                .roles(role)
                .disabled(user.getUserStatus() == null || user.getUserStatus() != 0)
                .build();
    }
}
