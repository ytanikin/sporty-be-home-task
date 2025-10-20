package com.sporty.behometask.service;

import com.sporty.behometask.client.AviationDataClient;
import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.exception.InvalidIcaoCodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.regex.Pattern;

/**
 * Service layer for airport operations.
 * Contains business logic and validation, delegates to the client layer for external calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AirportService {
    
    private static final Pattern ICAO_CODE_PATTERN = Pattern.compile("^[A-Z]{4}$");
    
    private final AviationDataClient aviationDataClient;
    
    /**
     * Retrieves airport information by ICAO code.
     * Validates the ICAO code format before making the external API call.
     * 
     * @param icaoCode the 4-letter ICAO airport code
     * @return airport information
     * @throws InvalidIcaoCodeException if the ICAO code format is invalid
     * @throws com.sporty.behometask.exception.AirportNotFoundException if airport not found
     * @throws com.sporty.behometask.exception.AviationApiException if API call fails
     */
    @Cacheable(cacheNames = "airports", key = "#icaoCode")
    public AirportResponse getAirportByIcaoCode(String icaoCode) {
        log.info("Received request for airport with ICAO code: {}", icaoCode);
        String normalizedIcaoCode = icaoCode != null ? icaoCode.trim().toUpperCase() : "";
        validateIcaoCode(normalizedIcaoCode);
        return aviationDataClient.getAirportByIcaoCode(normalizedIcaoCode);
    }
    
    /**
     * Validates ICAO code format (4 uppercase letters).
     * 
     * @param icaoCode the ICAO code to validate
     * @throws InvalidIcaoCodeException if the code format is invalid
     */
    private void validateIcaoCode(String icaoCode) {
        if (icaoCode == null || icaoCode.isEmpty()) {
            throw new InvalidIcaoCodeException("ICAO code cannot be empty");
        }
        
        if (!ICAO_CODE_PATTERN.matcher(icaoCode).matches()) {
            throw new InvalidIcaoCodeException(String.format("Invalid ICAO code format: '%s'. ICAO codes must be exactly 4 uppercase letters.", icaoCode));
        }
    }
}


