package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private ManagerService managerService;

    // 특정 userId를 작성자로 가지는 Todo mock 생성 헬퍼
    private Todo mockTodoOwnedBy(long ownerId) {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        Todo todo = mock(Todo.class);
        when(todo.getUser()).thenReturn(owner);
        return todo;
    }

    // ===== saveManager =====

    @Test
    void 담당자_등록에_성공한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(2L);

        Todo todo = mockTodoOwnedBy(userId);

        User managerUser = mock(User.class);
        when(managerUser.getId()).thenReturn(2L);
        when(managerUser.getEmail()).thenReturn("manager@example.com");

        Manager savedManager = mock(Manager.class);
        when(savedManager.getId()).thenReturn(1L);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(managerRepository.save(any(Manager.class))).thenReturn(savedManager);

        // when
        ManagerSaveResponse response = managerService.saveManager(userId, todoId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUser().getId()).isEqualTo(2L);
        assertThat(response.getUser().getEmail()).isEqualTo("manager@example.com");
        verify(managerRepository).save(any(Manager.class));
    }

    @Test
    void 일정이_존재하지_않으면_담당자_등록_시_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 999L;
        ManagerSaveRequest request = new ManagerSaveRequest(2L);

        when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> managerService.saveManager(userId, todoId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Todo not found");

        verify(userRepository, never()).findById(any());
        verify(managerRepository, never()).save(any());
    }

    @Test
    void 일정_작성자가_아니면_담당자_등록_시_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(2L);

        // 다른 유저(99L)가 작성한 일정
        Todo todo = mockTodoOwnedBy(99L);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

        // when & then
        assertThatThrownBy(() -> managerService.saveManager(userId, todoId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("담당자를 등록하려고 하는 유저가 유효하지 않거나, 일정을 만든 유저가 아닙니다.");

        verify(userRepository, never()).findById(any());
        verify(managerRepository, never()).save(any());
    }

    @Test
    void 담당자로_등록하려는_유저가_존재하지_않으면_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(2L);

        Todo todo = mockTodoOwnedBy(userId);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> managerService.saveManager(userId, todoId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("등록하려고 하는 담당자 유저가 존재하지 않습니다.");

        verify(managerRepository, never()).save(any());
    }

    @Test
    void 일정_작성자는_본인을_담당자로_등록할_수_없다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(userId);  // 본인 id로 등록 시도

        Todo todo = mockTodoOwnedBy(userId);

        User managerUser = mock(User.class);
        when(managerUser.getId()).thenReturn(userId);  // 본인과 동일

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(userRepository.findById(userId)).thenReturn(Optional.of(managerUser));

        // when & then
        assertThatThrownBy(() -> managerService.saveManager(userId, todoId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("일정 작성자는 본인을 담당자로 등록할 수 없습니다.");

        verify(managerRepository, never()).save(any());
    }

    // ===== getManagers =====

    @Test
    void 담당자_목록_조회에_성공한다() {
        // given
        long todoId = 1L;

        Todo todo = mock(Todo.class);
        when(todo.getId()).thenReturn(todoId);

        User user1 = mock(User.class);
        when(user1.getId()).thenReturn(1L);
        when(user1.getEmail()).thenReturn("user1@example.com");

        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(2L);
        when(user2.getEmail()).thenReturn("user2@example.com");

        Manager manager1 = mock(Manager.class);
        when(manager1.getId()).thenReturn(1L);
        when(manager1.getUser()).thenReturn(user1);

        Manager manager2 = mock(Manager.class);
        when(manager2.getId()).thenReturn(2L);
        when(manager2.getUser()).thenReturn(user2);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(managerRepository.findByTodoIdWithUser(todoId)).thenReturn(List.of(manager1, manager2));

        // when
        List<ManagerResponse> result = managerService.getManagers(todoId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getUser().getId()).isEqualTo(1L);
        assertThat(result.get(0).getUser().getEmail()).isEqualTo("user1@example.com");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getUser().getEmail()).isEqualTo("user2@example.com");
    }

    @Test
    void 일정이_존재하지_않으면_담당자_목록_조회_시_예외가_발생한다() {
        // given
        long todoId = 999L;
        when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> managerService.getManagers(todoId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("할일을 찾을 수 없습니다.");

        verify(managerRepository, never()).findByTodoIdWithUser(any());
    }

    // ===== deleteManager =====

    @Test
    void 담당자_삭제에_성공한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        long managerId = 1L;

        Todo todo = mockTodoOwnedBy(userId);
        when(todo.getId()).thenReturn(todoId);

        Manager manager = mock(Manager.class);
        when(manager.getTodo()).thenReturn(todo);  // 같은 todo → id 일치

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(managerRepository.findById(managerId)).thenReturn(Optional.of(manager));

        // when
        managerService.deleteManager(userId, todoId, managerId);

        // then
        verify(managerRepository).delete(manager);
    }

    @Test
    void 일정이_존재하지_않으면_담당자_삭제_시_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 999L;
        long managerId = 1L;

        when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> managerService.deleteManager(userId, todoId, managerId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Todo not found");

        verify(managerRepository, never()).findById(any());
        verify(managerRepository, never()).delete(any());
    }

    @Test
    void 일정_작성자가_아니면_담당자_삭제_시_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        long managerId = 1L;

        // 다른 유저(99L)가 작성한 일정
        Todo todo = mockTodoOwnedBy(99L);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

        // when & then
        assertThatThrownBy(() -> managerService.deleteManager(userId, todoId, managerId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("해당 일정을 만든 유저가 유효하지 않습니다.");

        verify(managerRepository, never()).findById(any());
        verify(managerRepository, never()).delete(any());
    }

    @Test
    void 담당자가_존재하지_않으면_삭제_시_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        long managerId = 999L;

        Todo todo = mockTodoOwnedBy(userId);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(managerRepository.findById(managerId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> managerService.deleteManager(userId, todoId, managerId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Manager not found");

        verify(managerRepository, never()).delete(any());
    }

    @Test
    void 해당_일정의_담당자가_아니면_삭제_시_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        long managerId = 1L;

        Todo todo = mockTodoOwnedBy(userId);
        when(todo.getId()).thenReturn(todoId);

        // manager가 다른 일정(999L)에 속함
        Todo differentTodo = mock(Todo.class);
        when(differentTodo.getId()).thenReturn(999L);

        Manager manager = mock(Manager.class);
        when(manager.getTodo()).thenReturn(differentTodo);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(managerRepository.findById(managerId)).thenReturn(Optional.of(manager));

        // when & then
        assertThatThrownBy(() -> managerService.deleteManager(userId, todoId, managerId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("해당 일정에 등록된 담당자가 아닙니다.");

        verify(managerRepository, never()).delete(any());
    }
}
