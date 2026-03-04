package org.example.expert.domain.comment.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.dto.response.CommentWithUserResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.exception.InvalidRequestException;
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
class CommentServiceTest {

    @Mock
    private TodoRepository todoRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    // ===== saveComment =====

    @Test
    void 댓글_저장에_성공한다() {
        // given
        long userId = 1L;
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("댓글 내용");

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");

        Todo todo = mock(Todo.class);

        Comment savedComment = mock(Comment.class);
        when(savedComment.getId()).thenReturn(1L);
        when(savedComment.getContents()).thenReturn("댓글 내용");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // when
        CommentSaveResponse response = commentService.saveComment(userId, todoId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getContents()).isEqualTo("댓글 내용");
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void 유저가_존재하지_않으면_댓글_저장_시_예외가_발생한다() {
        // given
        long userId = 999L;
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("댓글 내용");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.saveComment(userId, todoId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(todoRepository, never()).findById(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void 일정이_존재하지_않으면_댓글_저장_시_예외가_발생한다() {
        // given
        long userId = 1L;
        long todoId = 999L;
        CommentSaveRequest request = new CommentSaveRequest("댓글 내용");

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.saveComment(userId, todoId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Todo not found");

        verify(commentRepository, never()).save(any());
    }

    // ===== getComments =====

    @Test
    void 댓글_목록_조회에_성공한다() {
        // given
        long todoId = 1L;
        List<CommentWithUserResponse> comments = List.of(
                new CommentWithUserResponse(1L, "첫 번째 댓글", 1L, "user1@example.com"),
                new CommentWithUserResponse(2L, "두 번째 댓글", 2L, "user2@example.com")
        );
        when(commentRepository.findDtoByTodoIdWithUser(todoId)).thenReturn(comments);

        // when
        List<CommentWithUserResponse> result = commentService.getComments(todoId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCommentId()).isEqualTo(1L);
        assertThat(result.get(0).getContents()).isEqualTo("첫 번째 댓글");
        assertThat(result.get(0).getEmail()).isEqualTo("user1@example.com");
        assertThat(result.get(1).getCommentId()).isEqualTo(2L);
        assertThat(result.get(1).getContents()).isEqualTo("두 번째 댓글");
        assertThat(result.get(1).getEmail()).isEqualTo("user2@example.com");
    }
}
