package org.example.expert.domain.user.controller;

import org.example.expert.RestDocsSupport;
import org.example.expert.config.JwtFilter;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.service.UserAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity
class UserAdminControllerTest extends RestDocsSupport {

    @MockBean
    private UserAdminService userAdminService;

    @MockBean
    private JwtFilter jwtFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor withRole(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    @Test
    void 역할_변경에_성공한다() throws Exception {
        // given
        long userId = 1L;
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

        // then
        mockMvc.perform(patch("/admin/users/{userId}", userId)
                        .with(withRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(restDocsHandler("user/change-role"));
    }

    @Test
    void ADMIN_권한이_없으면_403이_발생한다() throws Exception {
        // given
        long userId = 1L;
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

        // then
        mockMvc.perform(patch("/admin/users/{userId}", userId)
                        .with(withRole("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(restDocsHandler("user/change-role/exceptions/unauthorized"));
    }

    @Test
    void 존재하지_않는_유저이면_400이_발생한다() throws Exception {
        // given
        long userId = 999L;
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

        // when
        doThrow(new InvalidRequestException("User not found"))
                .when(userAdminService).changeUserRole(eq(userId), any(UserRoleChangeRequest.class));

        // then
        mockMvc.perform(patch("/admin/users/{userId}", userId)
                        .with(withRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andDo(restDocsHandler("user/change-role/exceptions/user-not-found"));
    }

    @Test
    void 유효하지_않은_역할이면_400이_발생한다() throws Exception {
        // given
        long userId = 1L;
        UserRoleChangeRequest request = new UserRoleChangeRequest("INVALID_ROLE");

        // when
        doThrow(new InvalidRequestException("유효하지 않은 UserRole"))
                .when(userAdminService).changeUserRole(eq(userId), any(UserRoleChangeRequest.class));

        // then
        mockMvc.perform(patch("/admin/users/{userId}", userId)
                        .with(withRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 UserRole"))
                .andDo(restDocsHandler("user/change-role/exceptions/invalid-role"));
    }
}
