package com.team9.jobbotdari.kafka.messagequeue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team9.jobbotdari.entity.Log;
import com.team9.jobbotdari.entity.User;
import com.team9.jobbotdari.kafka.dto.LogMessage;
import com.team9.jobbotdari.repository.LogRepository;
import com.team9.jobbotdari.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka로부터 로그 메시지를 수신(consume)하여
 * DB(Log 테이블)에 저장하는 역할을 하는 Kafka 컨슈머 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLogConsumer {

    // 로그 데이터를 저장할 JPA 리포지토리 (Log 엔티티용)
    private final LogRepository logRepository;

    // 메시지에 포함된 userId로 유저를 조회하기 위한 JPA 리포지토리
    private final UserRepository userRepository;

    // Kafka 메시지(JSON 문자열)를 Java 객체로 변환하기 위한 Jackson ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kafka 토픽("log-topic")으로부터 메시지를 수신하고 처리하는 메서드
     * - 메시지는 JSON 문자열로 들어오며, LogMessage 객체로 역직렬화됨
     *
     * @param kafkaMessage Kafka 브로커로부터 전달된 JSON 문자열 메시지
     */
    @KafkaListener(topics = "log-topic")
    public void consume(String kafkaMessage) {
        try {
            // JSON 문자열을 LogMessage DTO로 변환
            LogMessage message = objectMapper.readValue(kafkaMessage, LogMessage.class);
            log.info("KafkaLogConsumer received message: {}", message);

            // 메시지에 userId가 있을 경우 DB에서 해당 유저 조회 (nullable 허용)
            User user = null;
            if (message.getUserId() != null) {
                user = userRepository.findById(message.getUserId()).orElse(null);
            }

            // 변환된 메시지 정보를 기반으로 Log 엔티티 생성
            Log logEntity = Log.builder()
                    .user(user)  // 조회된 유저 객체 (없을 경우 null)
                    .action(message.getAction())  // 수행된 동작 (ex: ERROR in UserService.login)
                    .description(message.getDescription())  // 상세 설명 (예외 메시지 등)
                    .build();

            // 로그 데이터를 DB에 저장
            logRepository.save(logEntity);
        } catch (JsonProcessingException e) {
            // JSON 역직렬화 실패 시 로그로 출력 (메시지 무시)
            log.error("Kafka 메시지 처리 중 오류 발생", e);
        }
    }
}