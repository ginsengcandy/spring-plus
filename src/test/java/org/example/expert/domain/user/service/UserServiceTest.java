package org.example.expert.domain.user.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ===== getUser =====

    @Test
    void 유저_조회에_성공한다() {
        // given
        long userId = 1L;

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        UserResponse response = userService.getUser(userId);

        // then
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 유저가_존재하지_않으면_조회_시_예외가_발생한다() {
        // given
        long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    // ===== changePassword =====

    @Test
    void 비밀번호_변경에_성공한다() {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPass1!", "NewPass2@");

        User user = mock(User.class);
        when(user.getPassword()).thenReturn("encodedOldPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "encodedOldPassword")).thenReturn(true);   // 이전 pw 인증 통과
        when(passwordEncoder.matches("NewPass2@", "encodedOldPassword")).thenReturn(false);  // 새 pw != 기존 pw
        when(passwordEncoder.encode("NewPass2@")).thenReturn("encodedNewPassword");

        // when
        userService.changePassword(userId, request);

        // then
        verify(user).changePassword("encodedNewPassword");
    }

    @Test
    void 새_비밀번호가_8자_미만이면_예외가_발생한다() {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPass1!", "Short1!");  // 7자

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void 새_비밀번호에_숫자가_없으면_예외가_발생한다() {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPass1!", "NoNumber!");

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void 새_비밀번호에_대문자가_없으면_예외가_발생한다() {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPass1!", "nouppercase1!");

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void 유저가_존재하지_않으면_비밀번호_변경_시_예외가_발생한다() {
        // given
        long userId = 999L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPass1!", "NewPass2@");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void 이전_비밀번호가_틀리면_예외가_발생한다() {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("WrongOld1!", "NewPass2@");

        User user = mock(User.class);
        when(user.getPassword()).thenReturn("encodedOldPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongOld1!", "encodedOldPassword")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("현재 비밀번호를 잘못 입력했습니다.");

        verify(user, never()).changePassword(any());
    }

    @Test
    void 새_비밀번호가_기존_비밀번호와_같으면_예외가_발생한다() {
        // given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPass1!", "NewPass2@");

        User user = mock(User.class);
        when(user.getPassword()).thenReturn("encodedPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "encodedPassword")).thenReturn(true);  // 이전 pw 인증 통과
        when(passwordEncoder.matches("NewPass2@", "encodedPassword")).thenReturn(true);  // 새 pw == 기존 pw

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.");

        verify(user, never()).changePassword(any());
    }
}
