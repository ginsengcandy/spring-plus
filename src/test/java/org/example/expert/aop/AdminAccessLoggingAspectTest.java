package org.example.expert.aop;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccessLoggingAspectTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdminAccessLoggingAspect aspect;

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
    }

    private JoinPoint mockJoinPoint(String methodName) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getSignature()).thenReturn(signature);
        return joinPoint;
    }

    @Test
    void 어드바이스_실행_시_request의_userId와_URI를_읽는다() {
        // given
        JoinPoint joinPoint = mockJoinPoint("getUser");
        when(request.getAttribute("userId")).thenReturn(1L);
        when(request.getRequestURI()).thenReturn("/users/1");

        // when
        assertDoesNotThrow(() -> aspect.logAfterChangeUserRole(joinPoint));

        // then
        verify(request).getAttribute("userId");
        verify(request).getRequestURI();
    }

    @Test
    void userId_속성이_없어도_예외없이_실행된다() {
        // given - JwtFilter가 userId를 세팅하지 않은 경우
        JoinPoint joinPoint = mockJoinPoint("getUser");
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/users/1");

        // then - null을 "null" 문자열로 로깅하며 예외 없이 실행
        assertDoesNotThrow(() -> aspect.logAfterChangeUserRole(joinPoint));
    }

    @Test
    void 로그에_userId와_URL과_메서드명이_포함된다() {
        // given
        JoinPoint joinPoint = mockJoinPoint("getUser");
        when(request.getAttribute("userId")).thenReturn(42L);
        when(request.getRequestURI()).thenReturn("/users/42");

        // when
        aspect.logAfterChangeUserRole(joinPoint);

        // then
        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).hasSize(1);
        String message = logs.get(0).getFormattedMessage();
        assertThat(message)
                .contains("42")
                .contains("/users/42")
                .contains("getUser");
    }

    @Test
    void 로그_레벨이_INFO이다() {
        // given
        JoinPoint joinPoint = mockJoinPoint("getUser");
        when(request.getAttribute("userId")).thenReturn(1L);
        when(request.getRequestURI()).thenReturn("/users/1");

        // when
        aspect.logAfterChangeUserRole(joinPoint);

        // then
        assertThat(listAppender.list.get(0).getLevel())
                .isEqualTo(ch.qos.logback.classic.Level.INFO);
    }
}
