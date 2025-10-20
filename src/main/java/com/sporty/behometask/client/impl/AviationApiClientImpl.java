package com.sporty.behometask.client.impl;

import com.sporty.behometask.client.AviationDataClient;
import com.sporty.behometask.client.dto.AviationApiAirportResponse;
import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.exception.AirportNotFoundException;
import com.sporty.behometask.exception.AviationApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of AviationDataClient for Aviation Weather API. Includes resilience patterns: retry, circuit breaker, and rate limiting.
 */
@Slf4j
@Component
public class AviationApiClientImpl implements AviationDataClient {

    private final RestTemplate restTemplate;
    private final String apiBaseUrl;

    public AviationApiClientImpl(RestTemplate restTemplate, @Value("${aviation.api.base-url}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    @CircuitBreaker(name = "aviationApi", fallbackMethod = "getAirportFallback")
    @Retry(name = "aviationApi")
    @RateLimiter(name = "aviationApi")
    public AirportResponse getAirportByIcaoCode(String icaoCode) {
        log.info("Fetching airport data for ICAO code: {}", icaoCode);

        try {
            String url = String.format("%s/airport?ids=%s&format=json", apiBaseUrl, icaoCode);
            log.debug("Calling aviation API: {}", url);

            AviationApiAirportResponse[] apiResponseArray = restTemplate.getForObject(url, AviationApiAirportResponse[].class);

            if (apiResponseArray == null || apiResponseArray.length == 0) {
                log.warn("Received empty response for ICAO code: {}", icaoCode);
                throw new AirportNotFoundException(icaoCode);
            }

            AviationApiAirportResponse apiResponse = apiResponseArray[0];
            log.info("Successfully retrieved airport data for ICAO code: {}", icaoCode);
            return mapToAirportResponse(apiResponse);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Airport not found for ICAO code: {}", icaoCode);
            throw new AirportNotFoundException(icaoCode);
        } catch (HttpClientErrorException ex) {
            log.error("HTTP error calling aviation API for ICAO code: {}, status: {}", icaoCode, ex.getStatusCode(), ex);
            throw new AviationApiException(String.format("Aviation API returned error: %s", ex.getStatusCode()), ex);
        } catch (ResourceAccessException ex) {
            log.error("Timeout or connection error calling aviation API for ICAO code: {}", icaoCode, ex);
            throw new AviationApiException("Failed to connect to aviation API", ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling aviation API for ICAO code: {}", icaoCode, ex);
            throw new AviationApiException("Unexpected error calling aviation API", ex);
        }
    }

    /**
     * Fallback method for circuit breaker. Returns cached data or a meaningful error message.
     */
    private AirportResponse getAirportFallback(String icaoCode, Exception ex) {
        log.error("Circuit breaker fallback triggered for ICAO code: {}", icaoCode, ex);
        // In a real production system, we might return cached data here
        throw new AviationApiException("Aviation data service is currently unavailable", ex);
    }

    /**
     * Maps provider-specific response to our standardized airport response. This isolates the service layer from provider-specific data structures.
     */
    private AirportResponse mapToAirportResponse(AviationApiAirportResponse apiResponse) {
        AirportResponse.LocationInfo location = AirportResponse.LocationInfo.builder().latitude(apiResponse.getLatitude()).longitude(apiResponse.getLongitude())
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
}


