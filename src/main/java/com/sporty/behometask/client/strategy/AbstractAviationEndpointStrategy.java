package com.sporty.behometask.client.strategy;

import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.exception.AirportNotFoundException;
import com.sporty.behometask.exception.AviationApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Abstract base class for aviation endpoint strategies.
 * Provides common infrastructure (RestTemplate, baseUrl) and helper methods.
 * Each strategy implements getAirportByIcaoCode directly with its own resilience annotations.
 * 
 * @param <T> the response type from the API endpoint
 */
@Slf4j
public abstract class AbstractAviationEndpointStrategy<T> implements EndpointStrategy {
    
    protected final RestTemplate restTemplate;
    protected final String baseUrl;
    
    protected AbstractAviationEndpointStrategy(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Builds the API URL for the given ICAO code.
     * Each strategy can have a different URL format.
     * 
     * @param icaoCode the ICAO code
     * @return the complete API URL
     */
    protected abstract String buildUrl(String icaoCode);
    
    /**
     * Makes the API call and returns the raw response object.
     * 
     * @param url the API URL
     * @return the raw response from the API
     */
    protected abstract T executeApiCall(String url);
    
    /**
     * Maps the provider-specific response to our standardized AirportResponse.
     * 
     * @param apiResponse the raw response from the API
     * @param icaoCode the ICAO code used in the request
     * @return standardized AirportResponse
     */
    protected abstract AirportResponse mapToAirportResponse(T apiResponse, String icaoCode);
    
    /**
     * Validates the response and throws appropriate exceptions if needed.
     * 
     * @param apiResponse the raw response from the API
     * @param icaoCode the ICAO code used in the request
     * @throws AirportNotFoundException if airport not found
     */
    protected abstract void validateResponse(T apiResponse, String icaoCode) throws AirportNotFoundException;
    
    /**
     * Helper method to handle common exceptions and convert them to AviationApiException.
     * Can be used by subclasses in their getAirportByIcaoCode implementation.
     */
    protected void handleException(Exception ex, String icaoCode) {
        if (ex instanceof HttpClientErrorException.NotFound) {
            log.warn("[{}] Airport not found for ICAO code: {}", getStrategyName(), icaoCode);
            throw new AirportNotFoundException(icaoCode);
        } else if (ex instanceof HttpClientErrorException httpEx) {
            log.error("[{}] HTTP error calling API for ICAO code: {}, status: {}", getStrategyName(), icaoCode, httpEx.getStatusCode(), ex);
            throw new AviationApiException(String.format("Aviation API returned error: %s", httpEx.getStatusCode()), ex);
        } else if (ex instanceof ResourceAccessException) {
            log.error("[{}] Timeout or connection error calling API for ICAO code: {}", getStrategyName(), icaoCode, ex);
            throw new AviationApiException("Failed to connect to aviation API", ex);
        } else if (ex instanceof AirportNotFoundException || ex instanceof AviationApiException) {
            throw ex;
        } else {
            log.error("[{}] Unexpected error calling API for ICAO code: {}", getStrategyName(), icaoCode, ex);
            throw new AviationApiException("Unexpected error calling aviation API", ex);
        }
    }
}
