package com.mts.aadati.configs.aspect;

import com.mts.aadati.configs.security.details.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AopLogging {

    @Pointcut("execution(* com.mts.aadati.services.*.*(..))")
    public void services() {
    }

    @Around("services()")
    public Object servicesLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String userId = resolveCurrentUserId();
        long startTime = System.currentTimeMillis();

        log.info("UserId:{} Entering: {}", userId, methodName);

        try {
            Object result = joinPoint.proceed();
            log.info("UserId:{} Exiting: {} after {}ms", userId, methodName, System.currentTimeMillis() - startTime);
            return result;
        }
        catch (Exception e) {
            log.error("UserId:{} Exception in: {} after {}ms", userId, methodName, System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }


    private String resolveCurrentUserId(){
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
            || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof CustomUserDetails))
                return "anonymous";

            return ((CustomUserDetails) authentication.getPrincipal()).id().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
