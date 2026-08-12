package com.bbs.bbs.security;

import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.Mapper.BBSUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BbsUserDetailsServiceTest {
    @Mock
    BBSUserMapper userMapper;

    @Test
    void mapsDatabaseAccountStateAndRole() {
        BBSUser user = new BBSUser();
        user.setLoginName("admin@example.com");
        user.setPasswordHash("bcrypt-hash");
        user.setRole("ADMIN");
        user.setUserStatus((byte) 1);
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.any()))
                .thenReturn(user);

        UserDetails details = new BbsUserDetailsService(userMapper)
                .loadUserByUsername("admin@example.com");

        assertThat(details.getUsername()).isEqualTo("admin@example.com");
        assertThat(details.getPassword()).isEqualTo("bcrypt-hash");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(details.isEnabled()).isFalse();
    }
}
