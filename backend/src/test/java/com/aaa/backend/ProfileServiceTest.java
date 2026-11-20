package com.aaa.backend;

import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.file.FileStorageService;
import com.aaa.backend.profile.ProfileServiceImpl;
import com.aaa.backend.profile.dto.PasswordUpdateRequest;
import com.aaa.backend.profile.dto.PasswordChangeRequest;
import com.aaa.backend.entity.SysUser;
import com.aaa.backend.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {
    private final UserDao userDao = mock(UserDao.class);
    private final BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
    private final ProfileServiceImpl service = new ProfileServiceImpl(
            userDao, mock(LoginService.class), mock(FileStorageService.class), passwordEncoder);

    @Test
    void patientCanSetPasswordForSharedWebAccount() {
        LoginUser patient = new LoginUser(42L, "wx_abc", "Patient", null, null,
                null, null, "USER", null, 1);
        when(passwordEncoder.encode("web-pass-123")).thenReturn("bcrypt-hash");
        when(userDao.update(isNull(), any())).thenReturn(1);

        service.updatePassword(patient, new PasswordUpdateRequest("web-pass-123"));

        verify(passwordEncoder).encode("web-pass-123");
        verify(userDao).update(isNull(), any());
    }

    @Test
    void nonPatientCannotSetPasswordThroughPatientProfile() {
        LoginUser doctor = new LoginUser(7L, "doctor", "Doctor", null, null,
                null, null, "DOCTOR", 3L, 1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePassword(doctor, new PasswordUpdateRequest("web-pass-123")));

        assertEquals(BusinessErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(userDao, never()).update(isNull(), any());
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void bcryptByteLimitIsValidated() {
        LoginUser patient = new LoginUser(42L, "wx_abc", "Patient", null, null,
                null, null, "USER", null, 1);
        String multibytePassword = "长".repeat(25) + "abc";

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePassword(patient, new PasswordUpdateRequest(multibytePassword)));

        assertEquals(BusinessErrorCode.BAD_REQUEST, exception.getErrorCode());
        verify(userDao, never()).update(isNull(), any());
    }

    @Test
    void patientCanChangeSharedWebPasswordAfterCurrentPasswordVerification() {
        LoginUser patient = new LoginUser(42L, "wx_abc", "Patient", null, null,
                null, null, "USER", null, 1);
        SysUser user = new SysUser();
        user.setId(42L);
        user.setDeleted(0);
        user.setStatus(1);
        user.setRole("USER");
        user.setPassword("old-hash");
        when(userDao.selectById(42L)).thenReturn(user);
        when(passwordEncoder.matches("current-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password-123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");
        when(userDao.update(isNull(), any())).thenReturn(1);

        service.changePassword(patient, new PasswordChangeRequest("current-pass", "new-password-123"));

        verify(passwordEncoder).matches("current-pass", "old-hash");
        verify(passwordEncoder).encode("new-password-123");
        verify(userDao).update(isNull(), any());
    }

    @Test
    void incorrectCurrentPasswordDoesNotChangePassword() {
        LoginUser patient = new LoginUser(42L, "wx_abc", "Patient", null, null,
                null, null, "USER", null, 1);
        SysUser user = new SysUser();
        user.setId(42L);
        user.setDeleted(0);
        user.setStatus(1);
        user.setRole("USER");
        user.setPassword("old-hash");
        when(userDao.selectById(42L)).thenReturn(user);
        when(passwordEncoder.matches("wrong-pass", "old-hash")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.changePassword(patient,
                new PasswordChangeRequest("wrong-pass", "new-password-123")));

        verify(userDao, never()).update(isNull(), any());
        verify(passwordEncoder, never()).encode(any(String.class));
    }
}
