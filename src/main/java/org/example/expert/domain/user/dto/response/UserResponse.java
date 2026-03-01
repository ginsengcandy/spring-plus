package org.example.expert.domain.user.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class UserResponse {

    private final Long id;
    private final String email;

    @QueryProjection
    public UserResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }
}
