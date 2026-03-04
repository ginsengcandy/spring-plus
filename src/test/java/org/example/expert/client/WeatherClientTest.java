package org.example.expert.client;

import org.example.expert.domain.common.exception.ServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class WeatherClientTest {

    private static final String WEATHER_API_URL = "https://f-api.github.io/f-api/weather.json";

    private MockRestServiceServer mockServer;
    private WeatherClient weatherClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);

        weatherClient = new WeatherClient(builder);
    }

    @Test
    void 오늘_날씨를_성공적으로_조회한다() {
        // given
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        String body = String.format(
                "[{\"date\":\"%s\",\"weather\":\"Sunny\"},{\"date\":\"01-01\",\"weather\":\"Cloudy\"}]",
                today
        );

        mockServer.expect(requestTo(WEATHER_API_URL))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when
        String result = weatherClient.getTodayWeather();

        // then
        assertThat(result).isEqualTo("Sunny");
        mockServer.verify();
    }

    @Test
    void HTTP_응답_상태코드가_200이_아니면_ServerException이_발생한다() {
        // given - 201 Created: DefaultResponseErrorHandler가 예외를 던지지 않는 2xx 비-200 응답
        mockServer.expect(requestTo(WEATHER_API_URL))
                .andRespond(withStatus(HttpStatus.CREATED));

        // when & then
        assertThatThrownBy(() -> weatherClient.getTodayWeather())
                .isInstanceOf(ServerException.class)
                .hasMessageContaining("날씨 데이터를 가져오는데 실패했습니다.");

        mockServer.verify();
    }

    @Test
    void 응답_바디가_빈_배열이면_ServerException이_발생한다() {
        // given
        mockServer.expect(requestTo(WEATHER_API_URL))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> weatherClient.getTodayWeather())
                .isInstanceOf(ServerException.class)
                .hasMessage("날씨 데이터가 없습니다.");

        mockServer.verify();
    }

    @Test
    void 오늘_날짜에_해당하는_날씨가_없으면_ServerException이_발생한다() {
        // given - 오늘 날짜가 아닌 데이터만 포함
        String body = "[{\"date\":\"01-01\",\"weather\":\"Sunny\"},{\"date\":\"01-02\",\"weather\":\"Cloudy\"}]";

        mockServer.expect(requestTo(WEATHER_API_URL))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> weatherClient.getTodayWeather())
                .isInstanceOf(ServerException.class)
                .hasMessage("오늘에 해당하는 날씨 데이터를 찾을 수 없습니다.");

        mockServer.verify();
    }
}
