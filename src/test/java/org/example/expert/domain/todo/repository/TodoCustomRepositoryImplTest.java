package org.example.expert.domain.todo.repository;

import jakarta.persistence.EntityManager;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TodoCustomRepositoryImplTest {

    @Autowired
    private EntityManager em;

    private TodoCustomRepositoryImpl todoCustomRepository;

    @BeforeEach
    void setUp() {
        todoCustomRepository = new TodoCustomRepositoryImpl(em);
    }

    @Test
    void todoId로_일정을_조회하면_TodoResponse를_반환한다() {
        // given
        User user = new User("user@example.com", "password", UserRole.USER, "닉네임");
        em.persist(user);

        Todo todo = new Todo("제목", "내용", "Sunny", user);
        em.persist(todo);

        em.flush();
        em.clear();  // 1차 캐시 비워 실제 쿼리 실행 확인

        // when
        Optional<TodoResponse> result = todoCustomRepository.findByTodoId(todo.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(todo.getId());
        assertThat(result.get().getTitle()).isEqualTo("제목");
        assertThat(result.get().getContents()).isEqualTo("내용");
        assertThat(result.get().getWeather()).isEqualTo("Sunny");
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getUser().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 존재하지_않는_todoId로_조회하면_empty를_반환한다() {
        // when
        Optional<TodoResponse> result = todoCustomRepository.findByTodoId(999L);

        // then
        assertThat(result).isEmpty();
    }
}
