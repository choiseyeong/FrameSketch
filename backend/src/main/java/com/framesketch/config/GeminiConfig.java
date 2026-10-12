package com.framesketch.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini 챗 모델 빈.
 *
 * <p>{@code framesketch.gemini.api-key} 가 비어 있지 않을 때만 생성된다. 키가 없으면
 * 빈 자체가 등록되지 않으며, 목차 생성 서비스는 이를 감지해 503 을 돌려준다
 * ({@link com.framesketch.outline.OutlineGenerator} 참고).
 */
@Configuration
public class GeminiConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfig.class);

    @Bean
    @ConditionalOnProperty(name = "framesketch.gemini.api-key")
    public ChatModel geminiChatModel(
            @Value("${framesketch.gemini.api-key}") String apiKey,
            @Value("${framesketch.gemini.model}") String modelName) {

        log.info("Gemini 챗 모델을 초기화합니다. model={}", modelName);

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                // 창의성보다 지침 준수가 중요한 작업이라 낮게 둔다. 0 이면 지나치게 고정적인
                // 결과가 나와 재생성해도 거의 같은 목차가 나올 수 있어 0.3 으로 타협한다.
                .temperature(0.3)
                // 응답을 JSON 하나로만 받도록 강제한다. 설정 없이도 프롬프트로 유도할 수 있지만
                // 모델이 간혹 ```json 코드블록으로 감싸거나 설명을 덧붙이는데, 이 설정이 그 변동을
                // 거의 없애 준다. 그래도 완벽하지는 않아 파싱 쪽에 안전장치를 둔다.
                .responseFormat(ResponseFormat.builder().type(ResponseFormatType.JSON).build())
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
