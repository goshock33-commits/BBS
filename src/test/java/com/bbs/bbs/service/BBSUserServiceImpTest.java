package com.bbs.bbs.service;

import com.bbs.bbs.Mapper.BBSUserMapper;
import com.bbs.bbs.bean.BBSUser;
import com.bbs.bbs.common.Constants;
import com.bbs.bbs.web.dto.auth.RegisterRequest;
import com.bbs.bbs.web.dto.user.ChangePasswordRequest;
import com.bbs.bbs.web.dto.user.UpdateProfileRequest;
import com.bbs.bbs.web.error.BusinessException;
import com.bbs.bbs.web.error.ErrorCode;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BBSUserServiceImpTest {
    @Mock
    BBSUserMapper userMapper;
    @Mock
    HttpSession httpSession;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private BBSUserServiceImp service;

    @BeforeEach
    void setUp() {
        service = new BBSUserServiceImp(userMapper, encoder);
    }

    @Test
    void registerStoresBCryptHashAndDefaultAccountState() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(BBSUser.class))).thenReturn(1);

        service.register(new RegisterRequest(
                " User@Example.com ", "password123", " user ", "abcd"));

        ArgumentCaptor<BBSUser> captor = ArgumentCaptor.forClass(BBSUser.class);
        verify(userMapper).insert(captor.capture());
        BBSUser saved = captor.getValue();

        assertEquals("user@example.com", saved.getLoginName());
        assertEquals("user", saved.getNickName());
        assertTrue(encoder.matches("password123", saved.getPasswordHash()));
        assertEquals("USER", saved.getRole());
        assertEquals((byte) 0, saved.getUserStatus());
    }

    @Test
    void registerRejectsDuplicateNormalizedLoginName() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.register(new RegisterRequest(
                        " User@Example.com ", "password123", "user", "abcd")));

        assertEquals(ErrorCode.DUPLICATE_LOGIN_NAME, exception.getErrorCode());
    }

    @Test
    void registerReportsDatabaseWriteFailure() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(BBSUser.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.register(new RegisterRequest(
                        "user@example.com", "password123", "user", "abcd")));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
    }

    @Test
    void findActiveByLoginNameRejectsDisabledAccount() {
        BBSUser user = new BBSUser();
        user.setUserStatus((byte) 1);
        when(userMapper.selectOne(any())).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.findActiveByLoginName(" User@Example.com "));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.getErrorCode());
    }

    @Test
    void changePasswordVerifiesOriginalAndStoresNewBCryptHash() {
        BBSUser user = new BBSUser();
        user.setUserId(7L);
        user.setUserStatus((byte) 0);
        user.setPasswordHash(encoder.encode("old-password"));
        String oldHash = user.getPasswordHash();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateById(any(BBSUser.class))).thenReturn(1);

        service.changePassword(7L,
                new ChangePasswordRequest("old-password", "new-password"));

        assertNotEquals(oldHash, user.getPasswordHash());
        assertTrue(encoder.matches("new-password", user.getPasswordHash()));
        verify(userMapper).updateById(user);
    }

    @Test
    void changePasswordRejectsWrongOriginalPassword() {
        BBSUser user = new BBSUser();
        user.setUserId(7L);
        user.setUserStatus((byte) 0);
        user.setPasswordHash(encoder.encode("old-password"));
        when(userMapper.selectById(7L)).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.changePassword(7L,
                        new ChangePasswordRequest("wrong-password", "new-password")));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void changePasswordReportsDatabaseWriteFailure() {
        BBSUser user = new BBSUser();
        user.setUserId(7L);
        user.setUserStatus((byte) 0);
        user.setPasswordHash(encoder.encode("old-password"));
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateById(any(BBSUser.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.changePassword(7L,
                        new ChangePasswordRequest("old-password", "new-password")));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
    }

    @Test
    void legacyUpdatePasswordReturnsFalseWhenDatabaseWriteFails() {
        BBSUser user = new BBSUser();
        user.setUserId(7L);
        user.setUserStatus((byte) 0);
        user.setPasswordHash(encoder.encode("old-password"));
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateById(any(BBSUser.class))).thenReturn(0);

        assertFalse(service.updatePassword(7L, "old-password", "new-password"));
    }

    @Test
    void updateProfileReportsDatabaseWriteFailure() {
        BBSUser user = new BBSUser();
        user.setUserId(7L);
        user.setUserStatus((byte) 0);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateById(any(BBSUser.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateProfile(7L,
                        new UpdateProfileRequest("user", "未知", "北京", "简介")));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
    }

    @Test
    void registerRejectsPasswordLongerThanBcryptByteLimit() {
        String password = "中".repeat(25);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.register(new RegisterRequest(
                        "user@example.com", password, "user", "abcd")));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void recordSuccessfulLoginUpdatesOnlyIdentityAndTimestamp() {
        when(userMapper.updateById(any(BBSUser.class))).thenReturn(1);

        service.recordSuccessfulLogin(9L);

        ArgumentCaptor<BBSUser> captor = ArgumentCaptor.forClass(BBSUser.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertTrue(captor.getValue().getLastLoginTime() != null);
    }

    @Test
    void legacyLoginAcceptsPlaintextAndVerifiesBcryptHash() {
        BBSUser user = new BBSUser();
        user.setUserId(11L);
        user.setUserStatus((byte) 0);
        user.setPasswordHash(encoder.encode("password123"));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any(BBSUser.class))).thenReturn(1);

        String result = service.login(
                "user@example.com", "password123", httpSession);

        assertEquals("登录成功！", result);
        verify(httpSession).setAttribute(Constants.USER_SESSION_KEY, user);
        verify(userMapper).updateById(any(BBSUser.class));
    }
}
