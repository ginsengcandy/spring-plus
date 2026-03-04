package org.example.expert.domain.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.example.expert.config.CustomUserDetails;
import org.example.expert.config.JwtFilter;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
@AutoConfigureMockMvc(addFilters = false)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoService todoService;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void todo_단건_조회에_성공한다() throws Exception {
        // given
        long todoId = 1L;
        String title = "title";
        long userId = 1L;
        String email = "email@example.com";
        UserResponse userResponse = new UserResponse(userId, email);
        TodoResponse response = new TodoResponse(
                todoId,
                title,
                "contents",
                "Sunny",
                userResponse,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when
        when(todoService.getTodo(todoId)).thenReturn(response);

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다() throws Exception {
        // given
        long todoId = 100L;

        // when
        when(todoService.getTodo(todoId))
                .thenThrow(new InvalidRequestException("Todo not found"));

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    @Test
    void 할일_목록_조회에_성공한다() throws Exception {
        // given
        String weather = "맑음";
        UserResponse user = new UserResponse(1L, "user@example.com");
        LocalDateTime now = LocalDateTime.now();

        // 전체 3개 중 weather 조건에 해당하는 2개만 서비스가 반환
        List<TodoResponse> filteredTodos = List.of(
                new TodoResponse(1L, "제목1", "내용1", weather, user, now, now),
                new TodoResponse(2L, "제목2", "내용2", weather, user, now, now)
        );
        Page<TodoResponse> page = new PageImpl<>(filteredTodos);

        // when
        when(todoService.getTodos(weather, null, null, 1, 10)).thenReturn(page);

        // then
        mockMvc.perform(get("/todos").param("weather", weather))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].weather").value(weather))
                .andExpect(jsonPath("$.content[1].weather").value(weather));
    }

    @Test
    void 할일_추가에_성공한다() throws Exception {
        // given
        long userId = 1L;
        TodoSaveRequest request = new TodoSaveRequest("제목", "내용");
        UserResponse userResponse = new UserResponse(userId, "user@example.com");
        TodoSaveResponse response = new TodoSaveResponse(1L, "제목", "내용", "맑음", userResponse);

        CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
        when(mockUserDetails.getId()).thenReturn(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockUserDetails, null, List.of());

        // when
        when(todoService.saveTodo(eq(userId), any(TodoSaveRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(post("/todos")
                        .with(mockRequest -> {
                            SecurityContext context = SecurityContextHolder.createEmptyContext();
                            context.setAuthentication(auth);
                            SecurityContextHolder.setContext(context);
                            return mockRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("제목"))
                .andExpect(jsonPath("$.weather").value("맑음"));
    }

    @Test
    void 할일_추가_시_유저가_존재하지_않아_예외가_발생한다() throws Exception {
        // given
        long userId = 1L;
        TodoSaveRequest request = new TodoSaveRequest("제목", "내용");

        CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
        when(mockUserDetails.getId()).thenReturn(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockUserDetails, null, List.of());

        // when
        when(todoService.saveTodo(eq(userId), any(TodoSaveRequest.class)))
                .thenThrow(new EntityNotFoundException("존재하지 않는 유저입니다."));

        // then
        mockMvc.perform(post("/todos")
                        .with(mockRequest -> {
                            SecurityContext context = SecurityContextHolder.createEmptyContext();
                            context.setAuthentication(auth);
                            SecurityContextHolder.setContext(context);
                            return mockRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
