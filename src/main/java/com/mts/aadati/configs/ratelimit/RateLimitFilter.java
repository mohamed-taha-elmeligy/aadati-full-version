package com.mts.aadati.configs.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mts.aadati.utils.uris.Uri;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATHS = Uri.BASE + Uri.AUTH;
    private static final String EXCLUDED_PATHS ="/actuator";
    private static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String X_RATE_LIMIT = "X-RateLimit-Limit";
    private static final String OPTIONS = "OPTIONS";
    private static final String USER_PREFIX = "user:";
    private static final String IP_PREFIX = "ip:";


    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;


    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException
    {
        PolicyType policyType = resolvePolicy(request);
        String key = resolveKey(request,policyType);

        ConsumptionProbe consumptionProbe;

        try {
            consumptionProbe = rateLimitService.tryConsume(policyType,key);
        }
        catch (Exception e){
            log.error("Rate limiter unavailable, allowing request", e);
            filterChain.doFilter(request,response);
            return;
        }

        response.setHeader(X_RATE_LIMIT,String.valueOf(rateLimitService.capacityOf(policyType)));
        response.setHeader(X_RATE_LIMIT_REMAINING, String.valueOf(consumptionProbe.getRemainingTokens()));

        if (consumptionProbe.isConsumed()){
            filterChain.doFilter(request,response);
            return;
        }

        long retryAfterSeconds = Math.max(1,
                TimeUnit.NANOSECONDS.toSeconds(consumptionProbe.getNanosToWaitForRefill()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again in " + retryAfterSeconds + " seconds"
        );
        problem.setTitle("Too Many Requests");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problem));

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        if(OPTIONS.equalsIgnoreCase(request.getMethod()))
            return true;
        return request.getRequestURI().startsWith(EXCLUDED_PATHS);
    }

    private PolicyType resolvePolicy(HttpServletRequest request){
        if (request.getRequestURI().startsWith(AUTH_PATHS))
            return PolicyType.AUTH;
        return isAuthenticated() ? PolicyType.API : PolicyType.ANONYMOUS;
    }

    private String resolveKey(HttpServletRequest request, PolicyType policy){
        if (PolicyType.API.equals(policy))
            return USER_PREFIX + SecurityContextHolder.getContext().getAuthentication().getName();
        return IP_PREFIX + request.getRemoteAddr();
    }

    private boolean isAuthenticated(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
