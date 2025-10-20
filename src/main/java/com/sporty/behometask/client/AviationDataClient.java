package com.sporty.behometask.client;

import com.sporty.behometask.dto.AirportResponse;

/**
 * Interface for aviation data providers.
 * This abstraction allows easy switching between different aviation data providers
 * without changing the service layer implementation.
 */
public interface AviationDataClient {
    /**
     * Retrieves airport information by ICAO code.
     * 
     * @param icaoCode the 4-letter ICAO airport code
     * @return airport information
     * @throws com.sporty.behometask.exception.AirportNotFoundException if airport not found
     * @throws com.sporty.behometask.exception.AviationApiException if API call fails
     */
    AirportResponse getAirportByIcaoCode(String icaoCode);
}


