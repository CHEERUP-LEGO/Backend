package com.team9.jobbotdari.kafka.messagequeue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team9.jobbotdari.kafka.dto.LogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLogProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(String topic, LogMessage logMessage) {
        try {
            String json = objectMapper.writeValueAsString(logMessage);
            kafkaTemplate.send(topic, json);
            log.info("KafkaLogProducer sent message: {}", json);
        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패", e);
        }
    }
}