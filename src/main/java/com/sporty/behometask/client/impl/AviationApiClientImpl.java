package com.sporty.behometask.client.impl;

import com.sporty.behometask.client.AviationDataClient;
import com.sporty.behometask.client.strategy.EndpointStrategy;
import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.exception.AviationApiException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Implementation of AviationDataClient using strategy pattern with multiple endpoints.
 * Manages failover between different endpoint strategies with circuit breaker support.
 * Each strategy has its own resilience policies (retry, circuit breaker, rate limiter).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AviationApiClientImpl implements AviationDataClient {

    private final List<EndpointStrategy> strategies;
    
    private List<EndpointStrategy> getOrderedStrategies() {
        return strategies.stream()
                .sorted(Comparator.comparing(EndpointStrategy::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    public AirportResponse getAirportByIcaoCode(String icaoCode) {
        List<EndpointStrategy> orderedStrategies = getOrderedStrategies();
        AviationApiException lastException = null;
        int circuitBreakerOpenCount = 0;
        int totalStrategies = orderedStrategies.size();
        
        for (EndpointStrategy strategy : orderedStrategies) {
            try {
                log.debug("Attempting to fetch airport data using strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
                AirportResponse response = strategy.getAirportByIcaoCode(icaoCode);
                log.info("Successfully retrieved airport data using strategy: {}", strategy.getStrategyName());
                return response;
            } catch (com.sporty.behometask.exception.AirportNotFoundException ex) {
                log.warn("Airport not found using strategy: {}", strategy.getStrategyName());
                throw ex;
            } catch (CallNotPermittedException ex) {
                circuitBreakerOpenCount++;
                log.warn("Circuit breaker is OPEN for strategy: {} (priority: {}). Trying next strategy.", strategy.getStrategyName(), strategy.getPriority());
            } catch (BulkheadFullException ex) {
                log.warn("Bulkhead is FULL for strategy: {} (priority: {}). Trying next strategy.", strategy.getStrategyName(), strategy.getPriority(), ex);
                lastException = new AviationApiException("Strategy " + strategy.getStrategyName() + " is overloaded", ex);
            } catch (TimeoutException ex) {
                log.warn("Timeout occurred for strategy: {} (priority: {}). Trying next strategy.", strategy.getStrategyName(), strategy.getPriority(), ex);
                lastException = new AviationApiException("Strategy " + strategy.getStrategyName() + " timed out", ex);
            } catch (AviationApiException ex) {
                log.warn("Strategy {} (priority: {}) failed: {}", strategy.getStrategyName(), strategy.getPriority(), ex.getMessage());
                lastException = ex;
            } catch (Exception ex) {
                log.error("Unexpected error with strategy {} (priority: {}): {}", strategy.getStrategyName(), strategy.getPriority(), ex.getMessage(), ex);
                lastException = new AviationApiException("Unexpected error with strategy " + strategy.getStrategyName(), ex);
            }
        }
        
        if (circuitBreakerOpenCount == totalStrategies) {
            log.error("All circuit breakers are OPEN for ICAO code: {}. Returning fallback response.", icaoCode);
            return createFallbackResponse(icaoCode);
        }
        
        log.error("All endpoint strategies failed for ICAO code: {}", icaoCode);
        throw new AviationApiException("All aviation data endpoints are currently unavailable", lastException);
    }
    
    /**
     * Creates a fallback response when all circuit breakers are open.
     * 
     * @param icaoCode the ICAO code
     * @return fallback airport response indicating service unavailability
     */
    private AirportResponse createFallbackResponse(String icaoCode) {
        return AirportResponse.builder()
                .icaoCode(icaoCode)
                .name("Service Temporarily Unavailable")
                .city("N/A")
                .country("N/A")
                .timezone("UTC")
                .build();
    }
}
