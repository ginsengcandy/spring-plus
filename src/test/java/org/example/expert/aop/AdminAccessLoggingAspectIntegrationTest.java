package org.example.expert.aop;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.example.expert.config.CustomUserDetails;
import org.example.expert.config.JwtFilter;
import org.example.expert.domain.user.controller.UserController;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminAccessLoggingAspect.class)
@EnableAspectJAutoProxy
class AdminAccessLoggingAspectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtFilter jwtFilter;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUpLogger() {
        listAppender = new ListAppender<>();
        logger = (Logger) LoggerFactory.getLogger(AdminAccessLoggingAspect.class);
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDownLogger() {
        logger.detachAppender(listAppender);
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

    @Test
    void getUser_호출_시_Before_어드바이스가_실행된다() throws Exception {
        // given
        long userId = 1L;
        when(userService.getUser(userId)).thenReturn(new UserResponse(userId, "user@example.com"));

        // when
        mockMvc.perform(get("/users/{userId}", userId)
                        .requestAttr("userId", userId))
                .andExpect(status().isOk());

        // then - 어드바이스가 실행되어 로그가 1건 남아야 함
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getFormattedMessage())
                .contains("Admin Access Log");
    }

    @Test
    void 요청_속성의_userId와_URI가_로그에_기록된다() throws Exception {
        // given
        long userId = 99L;
        when(userService.getUser(userId)).thenReturn(new UserResponse(userId, "user@example.com"));

        // when
        mockMvc.perform(get("/users/{userId}", userId)
                        .requestAttr("userId", userId))
                .andExpect(status().isOk());

        // then
        String message = listAppender.list.get(0).getFormattedMessage();
        assertThat(message)
                .contains(String.valueOf(userId))
                .contains("/users/" + userId);
    }

    @Test
    void changePassword_호출_시_어드바이스가_실행되지_않는다() throws Exception {
        // given - changePassword는 포인트컷 대상이 아님
        long userId = 1L;

        // when
        mockMvc.perform(get("/users/{userId}", userId)  // getUser 대신 다른 시나리오 확인을 위해
                        .requestAttr("userId", userId))
                .andExpect(status().isOk());

        // 포인트컷이 getUser에만 적용됨을 확인:
        // getUser를 1번 호출하면 로그 1건만 생성
        assertThat(listAppender.list).hasSize(1);

        // 로그 초기화 후 changePassword 경로로 PUT 요청 시 로그 없음 확인
        listAppender.list.clear();

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/users")
                        .with(withAuth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"OldPass1!\",\"newPassword\":\"NewPass1!\"}"));

        assertThat(listAppender.list).isEmpty();
    }
}
