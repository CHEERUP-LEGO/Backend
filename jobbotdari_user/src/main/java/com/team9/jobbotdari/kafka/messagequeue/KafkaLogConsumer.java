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

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLogConsumer {
    private final LogRepository logRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "log-topic")
    public void consume(String kafkaMessage) {
        try {
            LogMessage message = objectMapper.readValue(kafkaMessage, LogMessage.class);
            log.info("KafkaLogConsumer received message: {}", message);

            User user = null;
            if (message.getUserId() != null) {
                user = userRepository.findById(message.getUserId()).orElse(null);
            }

            Log logEntity = Log.builder()
                    .user(user)
                    .action(message.getAction())
                    .description(message.getDescription())
                    .build();

            logRepository.save(logEntity);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 처리 중 오류 발생", e);
        }
    }
}