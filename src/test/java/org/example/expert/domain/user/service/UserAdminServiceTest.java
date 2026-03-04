package org.example.expert.domain.user.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    void 역할_변경에_성공한다() {
        // given
        long userId = 1L;
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userAdminService.changeUserRole(userId, request);

        // then
        verify(user).updateRole(UserRole.ADMIN);
    }

    @Test
    void 유저가_존재하지_않으면_역할_변경_시_예외가_발생한다() {
        // given
        long userId = 999L;
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userAdminService.changeUserRole(userId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("User not found");

        verify(userRepository).findById(userId);
    }

    @Test
    void 유효하지_않은_역할로_변경_시_예외가_발생한다() {
        // given
        long userId = 1L;
        UserRoleChangeRequest request = new UserRoleChangeRequest("INVALID_ROLE");

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userAdminService.changeUserRole(userId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("유효하지 않은 UserRole");

        verify(user, never()).updateRole(any());
    }
}
