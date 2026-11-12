package com.aaa.backend;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.auth.JwtBlacklistService;
import com.aaa.backend.auth.RoleAccessPolicy;
import com.aaa.backend.auth.dto.LoginUser;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.common.JwtUtil;
import com.aaa.backend.common.Result;
import com.aaa.backend.config.JwtInterceptor;
import com.aaa.backend.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationAuthorizationTest {
    private final LoginService loginService = mock(LoginService.class);
    private final JwtBlacklistService jwtBlacklistService = mock(JwtBlacklistService.class);
    private final JwtInterceptor interceptor = new JwtInterceptor(
            loginService,
            new CurrentUserContext(),
            JsonMapper.builder().build(),
            new RoleAccessPolicy(),
            jwtBlacklistService
    );

    @Test
    void unauthenticatedRequestsToApiAndLegacyAdminRoutesReturn401() throws Exception {
        assertUnauthorized("/api/appointments");
        assertUnauthorized("/admin/department/findALLDepartment");
    }

    @Test
    void patientTokenCannotAccessLegacyAdminRoutes() throws Exception {
        when(jwtBlacklistService.isRevoked(anyString())).thenReturn(false);
        when(loginService.currentUser(42L)).thenReturn(new LoginUser(
                42L, "patient", "患者", null, null, null, null, "USER", null, 1
        ));

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/admin/department/findALLDepartment"
        );
        request.addHeader("Authorization", "Bearer " + JwtUtil.createToken(Map.of("sub", "42")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"message\""));
    }

    @Test
    void legacyAndApiResponsesUseTheSameJsonFields() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();

        String legacyJson = mapper.writeValueAsString(Result.ok("payload"));
        String apiJson = mapper.writeValueAsString(ApiResponse.success("payload"));

        for (String json : new String[]{legacyJson, apiJson}) {
            assertTrue(json.contains("\"code\""));
            assertTrue(json.contains("\"data\""));
            assertTrue(json.contains("\"message\""));
            assertFalse(json.contains("\"msg\""));
        }
    }

    private void assertUnauthorized(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"message\""));
    }
}
