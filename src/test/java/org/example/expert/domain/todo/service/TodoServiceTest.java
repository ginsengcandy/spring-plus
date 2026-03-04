package org.example.expert.domain.todo.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;
    @Mock
    private WeatherClient weatherClient;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TodoService todoService;

    // ===== saveTodo =====

    @Test
    void 일정_저장에_성공한다() {
        // given
        long userId = 1L;
        TodoSaveRequest request = new TodoSaveRequest("제목", "내용");

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");

        Todo savedTodo = mock(Todo.class);
        when(savedTodo.getId()).thenReturn(1L);
        when(savedTodo.getTitle()).thenReturn("제목");
        when(savedTodo.getContents()).thenReturn("내용");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(weatherClient.getTodayWeather()).thenReturn("Sunny");
        when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

        // when
        TodoSaveResponse response = todoService.saveTodo(userId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("제목");
        assertThat(response.getContents()).isEqualTo("내용");
        assertThat(response.getWeather()).isEqualTo("Sunny");
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        verify(todoRepository).save(any(Todo.class));
    }

    @Test
    void 유저가_존재하지_않으면_일정_저장_시_예외가_발생한다() {
        // given
        long userId = 999L;
        TodoSaveRequest request = new TodoSaveRequest("제목", "내용");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> todoService.saveTodo(userId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("존재하지 않는 유저입니다.");

        verify(weatherClient, never()).getTodayWeather();
        verify(todoRepository, never()).save(any());
    }

    // ===== getTodos =====

    @Test
    void 일정_목록_조회에_성공한다() {
        // given
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("user@example.com");

        Todo todo = mock(Todo.class);
        when(todo.getId()).thenReturn(1L);
        when(todo.getTitle()).thenReturn("제목");
        when(todo.getContents()).thenReturn("내용");
        when(todo.getWeather()).thenReturn("Sunny");
        when(todo.getUser()).thenReturn(user);
        when(todo.getCreatedAt()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(todo.getModifiedAt()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));

        Page<Todo> todoPage = new PageImpl<>(List.of(todo), PageRequest.of(0, 10), 1);
        when(todoRepository.findAllByWeatherOrModifiedAt(any(), any(), any(), any()))
                .thenReturn(todoPage);

        // when
        Page<TodoResponse> result = todoService.getTodos("Sunny", "2024-01-01", "2024-12-31", 1, 10);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("제목");
        assertThat(result.getContent().get(0).getWeather()).isEqualTo("Sunny");
        assertThat(result.getContent().get(0).getUser().getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getUser().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 날짜_필터_없이_일정_목록_조회에_성공한다() {
        // given
        Page<Todo> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(todoRepository.findAllByWeatherOrModifiedAt(any(), any(), any(), any()))
                .thenReturn(emptyPage);

        // when
        Page<TodoResponse> result = todoService.getTodos(null, null, null, 1, 10);

        // then
        assertThat(result.getContent()).isEmpty();
        // startDate/endDate가 null이면 null이 그대로 전달되어야 함
        verify(todoRepository).findAllByWeatherOrModifiedAt(isNull(), isNull(), isNull(), any());
    }

    // ===== getTodo =====

    @Test
    void 일정_단건_조회에_성공한다() {
        // given
        long todoId = 1L;
        TodoResponse expected = new TodoResponse(
                todoId, "제목", "내용", "Sunny",
                new UserResponse(1L, "user@example.com"),
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 1, 0, 0)
        );
        when(todoRepository.findByTodoId(todoId)).thenReturn(Optional.of(expected));

        // when
        TodoResponse result = todoService.getTodo(todoId);

        // then
        assertThat(result.getId()).isEqualTo(todoId);
        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getWeather()).isEqualTo("Sunny");
        assertThat(result.getUser().getId()).isEqualTo(1L);
        assertThat(result.getUser().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 일정이_존재하지_않으면_단건_조회_시_예외가_발생한다() {
        // given
        long todoId = 999L;
        when(todoRepository.findByTodoId(todoId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> todoService.getTodo(todoId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Todo not found");
    }
}
