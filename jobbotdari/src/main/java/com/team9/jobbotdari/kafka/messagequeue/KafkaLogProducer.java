package com.team9.jobbotdari.kafka.messagequeue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team9.jobbotdari.kafka.dto.LogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka를 통해 로그 메시지를 전송(produce)하는 서비스 클래스
 * - 비즈니스 로직에서 발생한 로그를 Kafka 토픽으로 비동기 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLogProducer {

    // Kafka 메시지 전송을 위한 템플릿 (String key, String value 형식 사용)
    private final KafkaTemplate<String, String> kafkaTemplate;

    // LogMessage 객체를 JSON 문자열로 변환하기 위한 Jackson ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 로그 메시지를 Kafka로 전송하는 메서드
     *
     * @param topic       메시지를 보낼 Kafka 토픽 이름
     * @param logMessage  로그 내용 (userId, action, description 포함)
     */
    public void send(String topic, LogMessage logMessage) {
        try {
            // LogMessage 객체를 JSON 문자열로 직렬화
            String json = objectMapper.writeValueAsString(logMessage);

            // Kafka에 메시지를 비동기로 전송 (응답 대기 없이 처리됨)
            kafkaTemplate.send(topic, json);

            // 전송한 메시지를 로그로 출력 (디버깅 및 모니터링 용도)
            log.info("KafkaLogProducer sent message: {}", json);
        } catch (JsonProcessingException e) {
            // JSON 직렬화에 실패한 경우 예외 로그 출력
            log.error("JSON 직렬화 실패", e);
        }
    }
}