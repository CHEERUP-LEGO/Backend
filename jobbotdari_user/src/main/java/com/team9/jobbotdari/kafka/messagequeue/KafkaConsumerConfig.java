package com.team9.jobbotdari.kafka.messagequeue;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 메시지를 수신(consume)하기 위한 Kafka 컨슈머 설정 클래스
 * - KafkaListener를 사용할 수 있도록 설정을 제공하며,
 *   메시지를 자동으로 JSON 문자열로 받아올 수 있게 구성됨.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    /**
     * Kafka Consumer 설정 정보를 담은 ConsumerFactory Bean
     *
     * @return ConsumerFactory<String, String> - Kafka에서 String key/value 메시지를 수신하기 위한 팩토리
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Kafka 브로커 주소 (여러 개일 경우 쉼표로 구분 가능)
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "3.143.42.91:9092");

        // 컨슈머 그룹 ID 설정: 같은 그룹 내 컨슈머는 서로 파티션을 분할 소비함
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "log-group");

        // Kafka 오프셋 처리 방식 설정:
        // earliest → 가장 오래된 메시지부터 소비 (처음 구독 시 유용)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Kafka 메시지의 key를 디코딩할 디시리얼라이저 클래스 (문자열로 변환)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Kafka 메시지의 value를 디코딩할 디시리얼라이저 클래스 (문자열로 변환)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 설정을 기반으로 ConsumerFactory 생성 및 반환
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka 메시지를 수신할 리스너 컨테이너 팩토리 Bean
     * - @KafkaListener 메서드가 사용할 컨테이너 설정을 제공함
     *
     * @return ConcurrentKafkaListenerContainerFactory<String, String>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        // ConcurrentKafkaListenerContainerFactory는 멀티스레드 기반 메시지 소비가 가능함
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // 앞서 정의한 consumerFactory를 사용하여 메시지 수신
        factory.setConsumerFactory(consumerFactory());

        // 기본 concurrency(동시성)는 1 → 필요 시 factory.setConcurrency(n) 설정 가능
        return factory;
    }
}