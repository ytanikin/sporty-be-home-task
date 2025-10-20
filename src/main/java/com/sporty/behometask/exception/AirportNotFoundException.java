package com.sporty.behometask.exception;

/**
 * Exception thrown when an airport with the given ICAO code is not found.
 */
public class AirportNotFoundException extends RuntimeException {
    public AirportNotFoundException(String icaoCode) {
        super(String.format("Airport with ICAO code '%s' not found", icaoCode));
    }
}


