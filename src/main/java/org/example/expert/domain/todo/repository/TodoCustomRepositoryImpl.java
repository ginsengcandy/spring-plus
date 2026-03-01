package org.example.expert.domain.todo.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.dto.response.UserResponse;

import java.util.Optional;

import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

public class TodoCustomRepositoryImpl implements TodoCustomRepository {

    private final JPAQueryFactory queryFactory;

    public TodoCustomRepositoryImpl(EntityManager em) {queryFactory = new JPAQueryFactory(em);}

    @Override
    public Optional<TodoResponse> findByTodoId(Long todoId) {
        return Optional.ofNullable(
                queryFactory
                        .select(Projections.constructor(TodoResponse.class,
                                todo.id,
                                todo.title,
                                todo.contents,
                                todo.weather,
                                Projections.constructor(UserResponse.class,
                                        todo.user.id,
                                        todo.user.email),
                                todo.createdAt,
                                todo.modifiedAt
                                )
                        )
                        .from(todo)
                        .join(todo.user,user)
                        .where(todo.id.eq(todoId))
                        .fetchOne()
                );
    }
}
