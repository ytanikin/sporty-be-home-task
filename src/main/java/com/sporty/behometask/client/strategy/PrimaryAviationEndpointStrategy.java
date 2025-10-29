package com.sporty.behometask.client.strategy;

import com.sporty.behometask.client.dto.AviationApiAirportResponse;
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
 * Primary aviation endpoint strategy using the original Aviation Weather API.
 * This<｜place▁holder▁no▁478｜> strategy has its own resilience policies: retry, circuit breaker, and rate limiter.
 */
@Slf4j
@Component
public class PrimaryAviationEndpointStrategy extends AbstractAviationEndpointStrategy<AviationApiAirportResponse[]> {
    
    private static final String STRATEGY_NAME = "primary";
    private static final int PRIORITY = 1;
    
    public PrimaryAviationEndpointStrategy(RestTemplate restTemplate, @Value("${aviation.api.base-url}") String baseUrl) {
        super(restTemplate, baseUrl);
    }
    
    @Override
    protected String buildUrl(String icaoCode) {
        return String.format("%s/airport?ids=%s&format=json", baseUrl, icaoCode);
    }
    
    @Override
    @CircuitBreaker(name = "primaryAviationApi", fallbackMethod = "getAirportFallback")
    @Retry(name = "primaryAviationApi", fallbackMethod = "getRetryFallback")
    @RateLimiter(name = "primaryAviationApi")
    @TimeLimiter(name = "primaryAviationApi")
    @Bulkhead(name = "primaryAviationApi")
    @Timed(value = "aviation.api.primary", description = "Time taken to fetch airport data from primary API")
    public AirportResponse getAirportByIcaoCode(String icaoCode) {
        log.info("[{}] Fetching airport data for ICAO code: {}", getStrategyName(), icaoCode);
        
        try {
            String url = buildUrl(icaoCode);
            log.debug("[{}] Calling API: {}", getStrategyName(), url);
            
            AviationApiAirportResponse[] apiResponse = executeApiCall(url);
            validateResponse(apiResponse, icaoCode);
            
            AirportResponse result = mapToAirportResponse(apiResponse, icaoCode);
            log.info("[{}] Successfully retrieved airport data for ICAO code: {}", getStrategyName(), icaoCode);
            return result;
        } catch (Exception ex) {
            handleException(ex, icaoCode);
        }
    }
    
    @Override
    protected AviationApiAirportResponse[] executeApiCall(String url) {
        return restTemplate.getForObject(url, AviationApiAirportResponse[].class);
    }
    
    @Override
    protected void validateResponse(AviationApiAirportResponse[] apiResponse, String icaoCode) throws AirportNotFoundException {
        if (apiResponse == null || apiResponse.length == 0) {
            log.warn("[{}] Received empty response for ICAO code: {}", getStrategyName(), icaoCode);
            throw new AirportNotFoundException(icaoCode);
        }
    }
    
    @Override
    protected AirportResponse mapToAirportResponse(AviationApiAirportResponse[] apiResponse, String icaoCode) {
        AviationApiAirportResponse response = apiResponse[0];
        
        AirportResponse.LocationInfo location = AirportResponse.LocationInfo.builder()
                .latitude(response.getLatitude())
                .longitude(response.getLongitude())
                .build();
        
        return AirportResponse.builder()
                .icaoCode(response.getIcaoCode())
                .iataCode(response.getIataCode())
                .name(response.getName())
                .city(response.getCity())
                .country(response.getCountry())
                .location(location)
                .timezone("UTC")
                .elevation(response.getElevation())
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
        throw new AviationApiException("Primary aviation data service is currently unavailable", ex);
    }
    
    /**
     * Fallback method for retry exhaustion.
     */
    private AirportResponse getRetryFallback(String icaoCode, Exception ex) {
        log.error("[{}] Retry fallback triggered for ICAO code: {}", getStrategyName(), icaoCode, ex);
        throw new AviationApiException("Primary aviation data service failed after retries", ex);
    }
}
