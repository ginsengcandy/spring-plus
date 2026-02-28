package org.example.expert.domain.comment.dto.response;

import lombok.Getter;

@Getter
public class CommentWithUserResponse {
    private final long commentId;
    private final String contents;
    private final Long userId;
    private final String email;

    public CommentWithUserResponse(long commentId, String contents, Long userId, String email) {
        this.commentId = commentId;
        this.contents = contents;
        this.userId = userId;
        this.email = email;
    }
}
