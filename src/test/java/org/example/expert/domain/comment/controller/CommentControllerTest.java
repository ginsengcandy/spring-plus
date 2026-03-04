package org.example.expert.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.example.expert.config.CustomUserDetails;
import org.example.expert.config.JwtFilter;
import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.dto.response.CommentWithUserResponse;
import org.example.expert.domain.comment.service.CommentService;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.response.UserResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

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

    // POST /todos/{todoId}/comments

    @Test
    void 댓글_등록에_성공한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("댓글 내용");
        CommentSaveResponse response = new CommentSaveResponse(1L, "댓글 내용", new UserResponse(userId, "user@example.com"));

        // when
        when(commentService.saveComment(eq(userId), eq(todoId), any(CommentSaveRequest.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(post("/todos/{todoId}/comments", todoId)
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.contents").value("댓글 내용"))
                .andExpect(jsonPath("$.user.id").value(userId))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }

    @Test
    void 유저가_존재하지_않으면_댓글_등록_시_500이_발생한다() throws Exception {
        // given
        long userId = 999L;
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("댓글 내용");

        // when
        when(commentService.saveComment(eq(userId), eq(todoId), any(CommentSaveRequest.class)))
                .thenThrow(new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // then
        mockMvc.perform(post("/todos/{todoId}/comments", todoId)
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    void 일정이_존재하지_않으면_댓글_등록_시_400이_발생한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 999L;
        CommentSaveRequest request = new CommentSaveRequest("댓글 내용");

        // when
        when(commentService.saveComment(eq(userId), eq(todoId), any(CommentSaveRequest.class)))
                .thenThrow(new InvalidRequestException("Todo not found"));

        // then
        mockMvc.perform(post("/todos/{todoId}/comments", todoId)
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    // GET /todos/{todoId}/comments

    @Test
    void 댓글_목록_조회에_성공한다() throws Exception {
        // given
        long todoId = 1L;
        List<CommentWithUserResponse> response = List.of(
                new CommentWithUserResponse(1L, "첫 번째 댓글", 1L, "user1@example.com"),
                new CommentWithUserResponse(2L, "두 번째 댓글", 2L, "user2@example.com")
        );

        // when
        when(commentService.getComments(todoId)).thenReturn(response);

        // then
        mockMvc.perform(get("/todos/{todoId}/comments", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].commentId").value(1L))
                .andExpect(jsonPath("$[0].contents").value("첫 번째 댓글"))
                .andExpect(jsonPath("$[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$[1].commentId").value(2L))
                .andExpect(jsonPath("$[1].contents").value("두 번째 댓글"))
                .andExpect(jsonPath("$[1].email").value("user2@example.com"));
    }
}
