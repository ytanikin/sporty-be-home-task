package com.sporty.behometask.exception;

import com.sporty.behometask.dto.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Global exception handler for consistent error responses across the application.
 * Handles both business exceptions and technical failures with appropriate HTTP status codes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AirportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAirportNotFound(AirportNotFoundException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Airport not found - traceId: {}, message: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value()).error("Not Found")
                .message(ex.getMessage()).path(request.getRequestURI()).traceId(traceId).build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(InvalidIcaoCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIcaoCode(InvalidIcaoCodeException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Invalid ICAO code - traceId: {}, message: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value()).error("Bad Request")
                .message(ex.getMessage()).path(request.getRequestURI()).traceId(traceId).build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(AviationApiException.class)
    public ResponseEntity<ErrorResponse> handleAviationApiException(AviationApiException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.error("Aviation API error - traceId: {}, message: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable").message("Unable to retrieve airport data. Please try again later.").path(request.getRequestURI())
                .traceId(traceId).build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.error("Circuit breaker open - traceId: {}", traceId);
        
        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable").message("Service temporarily unavailable. Please try again later.").path(request.getRequestURI())
                .traceId(traceId).build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RequestNotPermitted ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Rate limit exceeded - traceId: {}", traceId);
        
        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests").message("Rate limit exceeded. Please try again later.").path(request.getRequestURI()).traceId(traceId).build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.error("Unexpected error - traceId: {}", traceId, ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error").message("An unexpected error occurred. Please try again later.").path(request.getRequestURI())
                .traceId(traceId).build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}


