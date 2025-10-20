package com.sporty.behometask.exception;

/**
 * Exception thrown when the upstream aviation API returns an error or is unavailable.
 */
public class AviationApiException extends RuntimeException {
    public AviationApiException(String message) {
        super(message);
    }
    
    public AviationApiException(String message, Throwable cause) {
        super(message, cause);
    }
}


