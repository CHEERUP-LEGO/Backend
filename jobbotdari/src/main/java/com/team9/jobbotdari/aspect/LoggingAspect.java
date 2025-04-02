package com.team9.jobbotdari.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team9.jobbotdari.kafka.dto.LogMessage;
import com.team9.jobbotdari.kafka.messagequeue.KafkaLogProducer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

/**
 * AOP 기반의 에러 로깅 Aspect 클래스
 * - Controller 또는 Service 계층에서 예외가 발생한 경우,
 *   해당 예외 정보를 Kafka를 통해 비동기로 로그 전송
 * - JWT 토큰에서 사용자 ID를 추출해 로그에 포함
 */
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    // 로깅용 SLF4J Logger
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Kafka로 로그를 전송하기 위한 프로듀서 의존성 주입
    private final KafkaLogProducer kafkaLogProducer;  // Kafka 프로듀서 주입

    /**
     * Controller 및 Service 계층의 모든 메서드를 감싸서 예외 발생 여부를 확인
     * 예외 발생 시 로그 메시지를 Kafka로 전송하고, 예외는 그대로 다시 던짐
     */
    @Around("execution(* com.team9.jobbotdari.controller..*(..)) " +
            "|| execution(* com.team9.jobbotdari.service..*(..))")
    public Object logOnlyOnError(ProceedingJoinPoint joinPoint) throws Throwable {
        // 현재 실행 중인 클래스 및 메서드 이름 추출
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = methodSignature.getMethod().getName();

        // 전달된 메서드 인자 목록 추출 (toString 기반)
        String args = Arrays.toString(joinPoint.getArgs());

        // 현재 요청자의 userId (JWT 토큰으로부터 추출)
        Long userId = getCurrentUserId();

        try {
            // 원래 메서드 실행 (예외 없을 경우 그냥 리턴)
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 예외 발생 시 Kafka 전송
            String errorAction = "ERROR in: " + className + "." + methodName;
            String errorDescription = "Arguments: " + args +
                    "\nException: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();

            // Kafka로 로그 메시지 전송 (비동기 처리)
            kafkaLogProducer.send("log-topic", LogMessage.builder()
                    .userId(userId)
                    .action(errorAction)
                    .description(errorDescription)
                    .build());

            // 콘솔/파일 로그에도 기록
            log.error("[LOG][ERROR] {} - {}", errorAction, errorDescription);

            // 예외는 호출자에게 다시 던져서 정상적인 에러 흐름 유지
            throw ex; // 예외는 그대로 다시 던짐
        }
    }

    /**
     * 현재 요청의 JWT 토큰에서 userId를 추출하는 메서드
     * - Authorization: Bearer {accessToken}
     * - JWT Payload를 Base64 디코딩하여 userId 추출
     *
     * @return userId (Long) 또는 추출 실패 시 null
     */
    private Long getCurrentUserId() {
        try {
            // 현재 요청 컨텍스트의 HttpServletRequest 가져오기
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return null;

            HttpServletRequest request = attributes.getRequest();

            // Authorization 헤더에서 Bearer 토큰 추출
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length < 2) return null;

                // JWT의 payload 부분(Base64 인코딩)을 디코딩
                String payload = parts[1];
                byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
                String jsonPayload = new String(decodedBytes, StandardCharsets.UTF_8);

                // JSON 파싱 → 클레임 Map 추출
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> claims = mapper.readValue(jsonPayload, Map.class);

                // userId 추출 (없을 경우 null)
                Object userIdObj = claims.get("userId");
                if (userIdObj != null) {
                    return Long.parseLong(userIdObj.toString());
                }
            }
        } catch (Exception e) {
            // JWT 파싱 실패 시 경고 로그 남기고 null 반환
            log.warn("UserId 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}