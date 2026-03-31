package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.AiSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 실제 운영 환경에서는 허용 도메인 지정 필요
public class AiController {

    private final AiSummaryService aiSummaryService;

    @PostMapping("/summarize")
    public Mono<Map<String, String>> summarize(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.isEmpty()) {
            return Mono.just(Map.of("summary", "요약할 내용이 없습니다."));
        }

        return aiSummaryService.summarize(content)
            .map(summary -> Map.of("summary", summary));
    }
}
