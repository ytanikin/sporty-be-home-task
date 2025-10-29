package com.sporty.behometask.client.strategy;

import com.sporty.behometask.dto.AirportResponse;

/**
 * Strategy interface for different aviation data endpoint implementations.
 * Each strategy represents a different endpoint with its own response format and resilience policies.
 */
public interface EndpointStrategy {
    /**
     * Retrieves airport information by ICAO code using this endpoint strategy.
     * 
     * @param icaoCode the 4-letter ICAO airport code
     * @return airport information
     * @throws com.sporty.behometask.exception.AirportNotFoundException if airport not found
     * @throws com.sporty.behometask.exception.AviationApiException if API call fails
     */
    AirportResponse getAirportByIcaoCode(String icaoCode);
    
    /**
     * Returns the name/identifier of this strategy for logging and configuration purposes.
     * 
     * @return strategy name
     */
    String getStrategyName();
    
    /**
     * Returns the priority/order of this strategy for failover sequencing.
     * Lower numbers indicate higher priority (tried first).
     * 
     * @return priority number
     */
    int getPriority();
}

