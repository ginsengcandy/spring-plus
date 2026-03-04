package org.example.expert.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.config.JwtFilter;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.auth.service.AuthService;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtFilter jwtFilter;

    // POST /auth/signup

    @Test
    void 회원가입에_성공한다() throws Exception {
        // given
        SignupRequest request = new SignupRequest("user@example.com", "Password1!", "USER", "닉네임");
        SignupResponse response = new SignupResponse("Bearer.token.value");

        // when
        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bearerToken").value("Bearer.token.value"));
    }

    @Test
    void 이미_존재하는_이메일로_회원가입_시_400이_발생한다() throws Exception {
        // given
        SignupRequest request = new SignupRequest("duplicate@example.com", "Password1!", "USER", "닉네임");

        // when
        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new InvalidRequestException("이미 존재하는 이메일입니다."));

        // then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."));
    }

    @Test
    void 유효하지_않은_역할로_회원가입_시_400이_발생한다() throws Exception {
        // given
        SignupRequest request = new SignupRequest("user@example.com", "Password1!", "INVALID", "닉네임");

        // when
        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new InvalidRequestException("유효하지 않은 UserRole"));

        // then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 UserRole"));
    }

    // POST /auth/signin

    @Test
    void 로그인에_성공한다() throws Exception {
        // given
        SigninRequest request = new SigninRequest("user@example.com", "Password1!");
        SigninResponse response = new SigninResponse("Bearer.token.value");

        // when
        when(authService.signin(any(SigninRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bearerToken").value("Bearer.token.value"));
    }

    @Test
    void 가입되지_않은_이메일로_로그인_시_400이_발생한다() throws Exception {
        // given
        SigninRequest request = new SigninRequest("unknown@example.com", "Password1!");

        // when
        when(authService.signin(any(SigninRequest.class)))
                .thenThrow(new InvalidRequestException("가입되지 않은 유저입니다."));

        // then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("가입되지 않은 유저입니다."));
    }

    @Test
    void 비밀번호가_틀리면_로그인_시_401이_발생한다() throws Exception {
        // given
        SigninRequest request = new SigninRequest("user@example.com", "WrongPassword1!");

        // when
        when(authService.signin(any(SigninRequest.class)))
                .thenThrow(new AuthException("잘못된 비밀번호입니다."));

        // then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("잘못된 비밀번호입니다."));
    }
}
