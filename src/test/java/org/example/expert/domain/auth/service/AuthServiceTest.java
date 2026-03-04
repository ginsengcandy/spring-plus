package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    // ===== signup =====

    @Test
    void 회원가입에_성공한다() {
        // given
        SignupRequest request = new SignupRequest("test@example.com", "Password1!", "USER", "닉네임");

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(1L);
        when(savedUser.getEmail()).thenReturn("test@example.com");
        when(savedUser.getNickname()).thenReturn("닉네임");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.createToken(1L, "test@example.com", UserRole.USER, "닉네임"))
                .thenReturn("Bearer.token.value");

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.getBearerToken()).isEqualTo("Bearer.token.value");
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).createToken(1L, "test@example.com", UserRole.USER, "닉네임");
    }

    @Test
    void 이미_존재하는_이메일로_회원가입_시_예외가_발생한다() {
        // given
        SignupRequest request = new SignupRequest("duplicate@example.com", "Password1!", "USER", "닉네임");
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("이미 존재하는 이메일입니다.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void 유효하지_않은_역할로_회원가입_시_예외가_발생한다() {
        // given
        SignupRequest request = new SignupRequest("test@example.com", "Password1!", "INVALID_ROLE", "닉네임");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // when & then
        // UserRole.of("INVALID_ROLE")에서 InvalidRequestException 발생
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("유효하지 않은 UserRole");

        verify(userRepository, never()).save(any());
    }

    // ===== signin =====

    @Test
    void 로그인에_성공한다() {
        // given
        SigninRequest request = new SigninRequest("test@example.com", "Password1!");
        User user = new User("test@example.com", "encodedPassword", UserRole.USER, "닉네임");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "encodedPassword")).thenReturn(true);
        when(jwtUtil.createToken(any(), eq("test@example.com"), eq(UserRole.USER), eq("닉네임")))
                .thenReturn("Bearer.token.value");

        // when
        SigninResponse response = authService.signin(request);

        // then
        assertThat(response.getBearerToken()).isEqualTo("Bearer.token.value");
    }

    @Test
    void 가입되지_않은_이메일로_로그인_시_예외가_발생한다() {
        // given
        SigninRequest request = new SigninRequest("unknown@example.com", "Password1!");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.signin(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("가입되지 않은 유저입니다.");
    }

    @Test
    void 비밀번호가_틀리면_로그인_시_예외가_발생한다() {
        // given
        SigninRequest request = new SigninRequest("test@example.com", "WrongPassword1!");
        User user = new User("test@example.com", "encodedPassword", UserRole.USER, "닉네임");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1!", "encodedPassword")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signin(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("잘못된 비밀번호입니다.");
    }
}
