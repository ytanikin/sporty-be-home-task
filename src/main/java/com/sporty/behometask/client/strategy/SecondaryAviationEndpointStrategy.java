package com.sporty.behometask.client.strategy;

import com.sporty.behometask.client.dto.SecondaryAviationApiResponse;
import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.exception.AirportNotFoundException;
import com.sporty.behometask.exception.AviationApiException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Secondary aviation endpoint strategy using example.com API.
 * This strategy has its own resilience policies: retry, circuit breaker, and rate limiter.
 */
@Slf4j
@Component
public class SecondaryAviationEndpointStrategy extends AbstractAviationEndpointStrategy<SecondaryAviationApiResponse> {
    
    private static final String STRATEGY_NAME = "secondary";
    private static final int PRIORITY = 2;
    
    public SecondaryAviationEndpointStrategy(RestTemplate restTemplate, @Value("${aviation.api.secondary.base-url}") String baseUrl) {
        super(restTemplate, baseUrl);
    }
    
    @Override
    protected String buildUrl(String icaoCode) {
        return String.format("%s/api/airports/%s", baseUrl, icaoCode);
    }
    
    @Override
    @CircuitBreaker(name = "secondaryAviationApi", fallbackMethod = "getAirportFallback")
    @Retry(name = "secondaryAviationApi", fallbackMethod = "getRetryFallback")
    @RateLimiter(name = "secondaryAviationApi")
    @TimeLimiter(name = "secondaryAviationApi")
    @Bulkhead(name = "secondaryAviationApi")
    @Timed(value = "aviation.api.secondary", description = "Time taken to fetch airport data from secondary API")
    public AirportResponse getAirportByIcaoCode(String icaoCode) {
        log.info("[{}] Fetching airport data for ICAO code: {}", getStrategyName(), icaoCode);
        
        try {
            String url = buildUrl(icaoCode);
            log.debug("[{}] Calling API: {}", getStrategyName(), url);
            
            SecondaryAviationApiResponse apiResponse = executeApiCall(url);
            validateResponse(apiResponse, icaoCode);
            
            AirportResponse result = mapToAirportResponse(apiResponse, icaoCode);
            log.info("[{}] Successfully retrieved airport data for ICAO code: {}", getStrategyName(), icaoCode);
            return result;
        } catch (Exception ex) {
            handleException(ex, icaoCode);
        }
    }
    
    @Override
    protected SecondaryAviationApiResponse executeApiCall(String url) {
        return restTemplate.getForObject(url, SecondaryAviationApiResponse.class);
    }
    
    @Override
    protected void validateResponse(SecondaryAviationApiResponse apiResponse, String icaoCode) throws AirportNotFoundException {
        if (apiResponse == null || apiResponse.getIcaoCode() == null) {
            log.warn("[{}] Received empty or invalid response for ICAO code: {}", getStrategyName(), icaoCode);
            throw new AirportNotFoundException(icaoCode);
        }
    }
    
    @Override
    protected AirportResponse mapToAirportResponse(SecondaryAviationApiResponse apiResponse, String icaoCode) {
        AirportResponse.LocationInfo location = AirportResponse.LocationInfo.builder()
                .latitude(apiResponse.getLatitude())
                .longitude(apiResponse.getLongitude())
                .build();
        
        return AirportResponse.builder()
                .icaoCode(apiResponse.getIcaoCode())
                .iataCode(apiResponse.getIataCode())
                .name(apiResponse.getName())
                .city(apiResponse.getCity())
                .country(apiResponse.getCountry())
                .location(location)
                .timezone("UTC")
                .elevation(apiResponse.getElevation())
                .build();
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    /**
     * Fallback method for circuit breaker.
     */
    private AirportResponse getAirportFallback(String icaoCode, Exception ex) {
        log.error("[{}] Circuit breaker fallback triggered for ICAO code: {}", getStrategyName(), icaoCode, ex);
        throw new AviationApiException("Secondary aviation data service is currently unavailable", ex);
    }
    
    /**
     * Fallback method for retry exhaustion.
     */
    private AirportResponse getRetryFallback(String icaoCode, Exception ex) {
        log.error("[{}] Retry fallback triggered for ICAO code: {}", getStrategyName(), icaoCode, ex);
        throw new AviationApiException("Secondary aviation data service failed after retries", ex);
    }
}
