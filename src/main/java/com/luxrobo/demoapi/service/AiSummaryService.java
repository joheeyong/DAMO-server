package com.luxrobo.demoapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiSummaryService {

    private final WebClient webClient;

    @Value("${google.ai.api-key}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public AiSummaryService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> summarize(String content) {
        log.info("Summarizing content with Gemini AI...");

        // Gemini API 요청 바디 구성
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", "다음 텍스트를 핵심 내용 위주로 3줄 이내로 요약해줘. 형식은 '- '로 시작하는 리스트 형태여야 해:\n\n" + content)
                ))
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 200
            )
        );

        return webClient.post()
            .uri(GEMINI_API_URL + apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(this::extractTextFromResponse)
            .onErrorReturn("요약을 생성하는 중 오류가 발생했습니다.");
    }

    private String extractTextFromResponse(Map response) {
        try {
            List candidates = (List) response.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            return (String) firstPart.get("text");
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return "요약을 파싱하는 데 실패했습니다.";
        }
    }
}
