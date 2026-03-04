package org.example.expert.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.example.expert.config.CustomUserDetails;
import org.example.expert.config.JwtFilter;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtFilter jwtFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor withAuth(long userId) {
        CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
        when(mockUserDetails.getId()).thenReturn(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockUserDetails, null, List.of());
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    @Test
    void 유저_단건_조회에_성공한다() throws Exception {
        // given
        long userId = 1L;
        UserResponse response = new UserResponse(userId, "user@example.com");

        // when
        when(userService.getUser(userId)).thenReturn(response);

        // then
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void 존재하지_않는_유저_조회_시_400이_발생한다() throws Exception {
        // given
        long userId = 999L;

        // when
        when(userService.getUser(userId))
                .thenThrow(new InvalidRequestException("User not found"));

        // then
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void 비밀번호_변경에_성공한다() throws Exception {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPassword1!", "NewPassword1!");

        // then
        mockMvc.perform(put("/users")
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void 존재하지_않는_유저의_비밀번호_변경_시_500이_발생한다() throws Exception {
        // given
        long userId = 999L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPassword1!", "NewPassword1!");

        // when
        doThrow(new EntityNotFoundException("User not found"))
                .when(userService).changePassword(eq(userId), any(UserChangePasswordRequest.class));

        // then
        mockMvc.perform(put("/users")
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void 새_비밀번호가_기존_비밀번호와_같으면_400이_발생한다() throws Exception {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("SamePassword1!", "SamePassword1!");

        // when
        doThrow(new InvalidRequestException("새 비밀번호는 기존 비밀번호와 같을 수 없습니다."))
                .when(userService).changePassword(eq(userId), any(UserChangePasswordRequest.class));

        // then
        mockMvc.perform(put("/users")
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("새 비밀번호는 기존 비밀번호와 같을 수 없습니다."));
    }

    @Test
    void 기존_비밀번호가_틀리면_400이_발생한다() throws Exception {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("WrongPassword1!", "NewPassword1!");

        // when
        doThrow(new InvalidRequestException("잘못된 비밀번호입니다."))
                .when(userService).changePassword(eq(userId), any(UserChangePasswordRequest.class));

        // then
        mockMvc.perform(put("/users")
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 비밀번호입니다."));
    }
}
