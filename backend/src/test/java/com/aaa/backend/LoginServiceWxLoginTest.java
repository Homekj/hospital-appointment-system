package com.aaa.backend;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.dao.UserDao;
import com.aaa.backend.service.serviceimpl.LoginServiceimpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class LoginServiceWxLoginTest {

    @Test
    void realWechatLoginFailsClearlyWhenMerchantAppCredentialsAreMissing() {
        LoginServiceimpl service = new LoginServiceimpl(
                mock(UserDao.class), mock(BCryptPasswordEncoder.class), JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "miniProgramAppId", " ");
        ReflectionTestUtils.setField(service, "miniProgramSecret", "");
        ReflectionTestUtils.setField(service, "mockWxLoginEnabled", false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.wxLogin("temporary-code", "患者"));

        assertEquals(BusinessErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        assertEquals("微信登录暂不可用", exception.getMessage());
    }
}
