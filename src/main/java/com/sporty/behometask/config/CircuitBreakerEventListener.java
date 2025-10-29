package com.sporty.behometask.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnFailureRateExceededEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Event listener for Circuit Breaker state transitions and events.
 * Provides real-time monitoring, alerting, and custom metrics for circuit breaker state changes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CircuitBreakerEventListener {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;
    
    // Store current failure rates for gauge metrics
    private final ConcurrentHashMap<String, AtomicReference<Double>> failureRates = new ConcurrentHashMap<>();

    /**
     * Registers event listeners for all circuit breakers after application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerCircuitBreakerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach((name, circuitBreaker) -> {
            circuitBreaker.getEventPublisher()
                    .onStateTransition(this::onStateTransition)
                    .onFailureRateExceeded(this::onFailureRateExceeded)
                    .onSlowCallRateExceeded(this::onSlowCallRateExceeded)
                    .onCallNotPermitted(this::onCallNotPermitted);
            
            // Initialize failure rate tracking
            failureRates.put(name, new AtomicReference<>(0.0));
            
            // Register gauge for failure rate
            Gauge.builder("circuit_breaker_failure_rate", failureRates.get(name), 
                    value -> value.get())
                    .tag("name", name)
                    .description("Circuit breaker failure rate percentage")
                    .register(meterRegistry);
            
            log.info("Registered event listeners for circuit breaker: {}", name);
        });
    }

    /**
     * Handles circuit breaker state transitions (CLOSED -> OPEN, OPEN -> HALF_OPEN, etc.).
     */
    private void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        String circuitBreakerName = event.getCircuitBreakerName();
        String fromState = event.getStateTransition().getFromState().name();
        String toState = event.getStateTransition().getToState().name();
        
        log.warn("Circuit breaker '{}' transitioned from {} to {}", 
                circuitBreakerName, fromState, toState);
        
        // Emit custom metric for state transition
        List<Tag> tags = Arrays.asList(
                Tag.of("name", circuitBreakerName),
                Tag.of("from", fromState),
                Tag.of("to", toState)
        );
        meterRegistry.counter("circuit_breaker_transitions", tags).increment();
        
        // Emit state transition metric with state value (for tracking current state)
        double stateValue = "OPEN".equals(toState) ? 2.0 : ("HALF_OPEN".equals(toState) ? 1.0 : 0.0);
        meterRegistry.gauge("circuit_breaker_state",
                Arrays.asList(Tag.of("name", circuitBreakerName), Tag.of("state", toState)),
                stateValue);
        
        // Log critical state transitions for alerting
        if ("OPEN".equals(toState)) {
            log.error("ALERT: Circuit breaker '{}' opened! Service is unavailable.", circuitBreakerName);
        } else if ("CLOSED".equals(toState) && "OPEN".equals(fromState)) {
            log.info("Circuit breaker '{}' recovered to CLOSED state.", circuitBreakerName);
        } else if ("HALF_OPEN".equals(toState)) {
            log.info("Circuit breaker '{}' entered HALF_OPEN state - testing recovery.", circuitBreakerName);
        }
    }

    /**
     * Handles failure rate threshold exceeded events.
     */
    private void onFailureRateExceeded(CircuitBreakerOnFailureRateExceededEvent event) {
        String circuitBreakerName = event.getCircuitBreakerName();
        float failureRate = event.getFailureRate();
        
        log.error("Circuit breaker '{}' failure rate exceeded: {}%", 
                circuitBreakerName, String.format("%.2f", failureRate));
        
        // Update failure rate value for gauge
        failureRates.computeIfAbsent(circuitBreakerName, k -> new AtomicReference<>(0.0))
                .set((double) failureRate);
    }

    /**
     * Handles slow call rate exceeded events.
     */
    private void onSlowCallRateExceeded(CircuitBreakerEvent event) {
        String circuitBreakerName = event.getCircuitBreakerName();
        
        log.warn("Circuit breaker '{}' slow call rate exceeded", circuitBreakerName);
        
        // Emit metric for slow call rate
        meterRegistry.counter("circuit_breaker_slow_calls",
                Arrays.asList(Tag.of("name", circuitBreakerName))).increment();
    }

    /**
     * Handles call not permitted events (when circuit is OPEN).
     */
    private void onCallNotPermitted(CircuitBreakerEvent event) {
        String circuitBreakerName = event.getCircuitBreakerName();
        
        log.debug("Circuit breaker '{}' call not permitted - circuit is OPEN", circuitBreakerName);
        
        // Emit metric for blocked calls
        meterRegistry.counter("circuit_breaker_calls_not_permitted",
                Arrays.asList(Tag.of("name", circuitBreakerName))).increment();
    }
}

