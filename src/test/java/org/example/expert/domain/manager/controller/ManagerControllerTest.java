package org.example.expert.domain.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.config.CustomUserDetails;
import org.example.expert.config.JwtFilter;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.service.ManagerService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ManagerService managerService;

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

    // POST /todos/{todoId}/managers

    @Test
    void 담당자_등록에_성공한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(2L);
        ManagerSaveResponse response = new ManagerSaveResponse(1L, new UserResponse(2L, "manager@example.com"));

        // when
        when(managerService.saveManager(eq(userId), eq(todoId), any(ManagerSaveRequest.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(post("/todos/{todoId}/managers", todoId)
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.user.id").value(2L))
                .andExpect(jsonPath("$.user.email").value("manager@example.com"));
    }

    @Test
    void 일정이_존재하지_않으면_담당자_등록_시_400이_발생한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 999L;
        ManagerSaveRequest request = new ManagerSaveRequest(2L);

        // when
        when(managerService.saveManager(eq(userId), eq(todoId), any(ManagerSaveRequest.class)))
                .thenThrow(new InvalidRequestException("Todo not found"));

        // then
        mockMvc.perform(post("/todos/{todoId}/managers", todoId)
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    @Test
    void 일정_작성자가_아니면_담당자_등록_시_400이_발생한다() throws Exception {
        // given
        long userId = 2L;
        long todoId = 1L;
        ManagerSaveRequest request = new ManagerSaveRequest(3L);

        // when
        when(managerService.saveManager(eq(userId), eq(todoId), any(ManagerSaveRequest.class)))
                .thenThrow(new InvalidRequestException("담당자를 등록하려고 하는 유저가 유효하지 않거나, 일정을 만든 유저가 아닙니다."));

        // then
        mockMvc.perform(post("/todos/{todoId}/managers", todoId)
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("담당자를 등록하려고 하는 유저가 유효하지 않거나, 일정을 만든 유저가 아닙니다."));
    }

    // GET /todos/{todoId}/managers

    @Test
    void 담당자_목록_조회에_성공한다() throws Exception {
        // given
        long todoId = 1L;
        List<ManagerResponse> response = List.of(
                new ManagerResponse(1L, new UserResponse(1L, "user1@example.com")),
                new ManagerResponse(2L, new UserResponse(2L, "user2@example.com"))
        );

        // when
        when(managerService.getManagers(todoId)).thenReturn(response);

        // then
        mockMvc.perform(get("/todos/{todoId}/managers", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].user.email").value("user1@example.com"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].user.email").value("user2@example.com"));
    }

    @Test
    void 일정이_존재하지_않으면_담당자_목록_조회_시_400이_발생한다() throws Exception {
        // given
        long todoId = 999L;

        // when
        when(managerService.getManagers(todoId))
                .thenThrow(new InvalidRequestException("할일을 찾을 수 없습니다."));

        // then
        mockMvc.perform(get("/todos/{todoId}/managers", todoId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("할일을 찾을 수 없습니다."));
    }

    // DELETE /todos/{todoId}/managers/{managerId}

    @Test
    void 담당자_삭제에_성공한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 1L;
        long managerId = 1L;

        // then
        mockMvc.perform(delete("/todos/{todoId}/managers/{managerId}", todoId, managerId)
                        .with(withAuth(userId)))
                .andExpect(status().isOk());
    }

    @Test
    void 일정이_존재하지_않으면_담당자_삭제_시_400이_발생한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 999L;
        long managerId = 1L;

        // when
        doThrow(new InvalidRequestException("Todo not found"))
                .when(managerService).deleteManager(eq(userId), eq(todoId), eq(managerId));

        // then
        mockMvc.perform(delete("/todos/{todoId}/managers/{managerId}", todoId, managerId)
                        .with(withAuth(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    @Test
    void 해당_일정의_담당자가_아니면_삭제_시_400이_발생한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 1L;
        long managerId = 999L;

        // when
        doThrow(new InvalidRequestException("해당 일정에 등록된 담당자가 아닙니다."))
                .when(managerService).deleteManager(eq(userId), eq(todoId), eq(managerId));

        // then
        mockMvc.perform(delete("/todos/{todoId}/managers/{managerId}", todoId, managerId)
                        .with(withAuth(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 일정에 등록된 담당자가 아닙니다."));
    }
}
