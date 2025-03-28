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

@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private final KafkaLogProducer kafkaLogProducer;  // Kafka 프로듀서 주입

    @Around("execution(* com.team9.jobbotdari.controller..*(..)) " +
            "|| execution(* com.team9.jobbotdari.service..*(..))")
    public Object logOnlyOnError(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = methodSignature.getMethod().getName();
        String args = Arrays.toString(joinPoint.getArgs());

        Long userId = getCurrentUserId();

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 예외 발생 시 Kafka 전송
            String errorAction = "ERROR in: " + className + "." + methodName;
            String errorDescription = "Arguments: " + args +
                    "\nException: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();

            kafkaLogProducer.send("log-topic", LogMessage.builder()
                    .userId(userId)
                    .action(errorAction)
                    .description(errorDescription)
                    .build());

            log.error("[LOG][ERROR] {} - {}", errorAction, errorDescription);

            throw ex; // 예외는 그대로 다시 던짐
        }
    }

    private Long getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return null;

            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length < 2) return null;

                String payload = parts[1];
                byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
                String jsonPayload = new String(decodedBytes, StandardCharsets.UTF_8);

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> claims = mapper.readValue(jsonPayload, Map.class);
                Object userIdObj = claims.get("userId");
                if (userIdObj != null) {
                    return Long.parseLong(userIdObj.toString());
                }
            }
        } catch (Exception e) {
            log.warn("UserId 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}
