package com.team9.jobbotdari.aspect;

import com.team9.jobbotdari.entity.Log;
import com.team9.jobbotdari.entity.User;
import com.team9.jobbotdari.repository.LogRepository;
import com.team9.jobbotdari.repository.UserRepository;
import com.team9.jobbotdari.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private final LogRepository logRepository;

    @Around("execution(* com.team9.jobbotdari.controller..*(..)) " +
            "|| execution(* com.team9.jobbotdari.service..*(..)) " +
            "|| (execution(* com.team9.jobbotdari.repository..*(..)) " +
            "    && !execution(* com.team9.jobbotdari.repository.LogRepository.*(..)))")
    public Object logOnlyOnError(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = methodSignature.getMethod().getName();
        String args = Arrays.toString(joinPoint.getArgs());

        User currentUser = getCurrentUser();

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            String errorAction = "ERROR in: " + className + "." + methodName;
            String errorDescription = "Arguments: " + args +
                    "\nException: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();

            logRepository.save(Log.builder()
                    .user(currentUser)
                    .action(errorAction)
                    .description(errorDescription)
                    .build());

            log.error("[LOG][ERROR] {} - {}", errorAction, errorDescription);
            throw ex;
        }
    }

    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                return ((CustomUserDetails) authentication.getPrincipal()).getUser();
            }
        } catch (Exception e) {
            log.warn("User 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}
