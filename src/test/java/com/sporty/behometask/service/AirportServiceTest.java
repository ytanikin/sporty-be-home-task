package com.sporty.behometask.service;

import com.sporty.behometask.client.AviationDataClient;
import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.exception.InvalidIcaoCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AirportService.
 * Verifies business logic and validation without external dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AirportServiceTest {
    
    @Mock
    private AviationDataClient aviationDataClient;
    
    private AirportService airportService;
    
    @BeforeEach
    void setUp() {
        airportService = new AirportService(aviationDataClient);
    }
    
    @Test
    void shouldSuccessfullyGetAirportByValidIcaoCode() {
        // Given
        String icaoCode = "KJFK";
        AirportResponse expectedResponse = AirportResponse.builder().icaoCode("KJFK").name("John F Kennedy International Airport").build();
        
        when(aviationDataClient.getAirportByIcaoCode(icaoCode)).thenReturn(expectedResponse);
        
        // When
        AirportResponse result = airportService.getAirportByIcaoCode(icaoCode);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIcaoCode()).isEqualTo("KJFK");
        verify(aviationDataClient, times(1)).getAirportByIcaoCode(icaoCode);
    }
    
    @Test
    void shouldNormalizeIcaoCodeToUppercase() {
        // Given
        String lowercaseIcaoCode = "kjfk";
        AirportResponse expectedResponse = AirportResponse.builder().icaoCode("KJFK").build();
        
        when(aviationDataClient.getAirportByIcaoCode("KJFK")).thenReturn(expectedResponse);
        
        // When
        AirportResponse result = airportService.getAirportByIcaoCode(lowercaseIcaoCode);
        
        // Then
        assertThat(result).isNotNull();
        verify(aviationDataClient, times(1)).getAirportByIcaoCode("KJFK");
    }
    
    @Test
    void shouldTrimWhitespaceFromIcaoCode() {
        // Given
        String icaoCodeWithSpaces = "  KJFK  ";
        AirportResponse expectedResponse = AirportResponse.builder().icaoCode("KJFK").build();
        
        when(aviationDataClient.getAirportByIcaoCode("KJFK")).thenReturn(expectedResponse);
        
        // When
        AirportResponse result = airportService.getAirportByIcaoCode(icaoCodeWithSpaces);
        
        // Then
        assertThat(result).isNotNull();
        verify(aviationDataClient, times(1)).getAirportByIcaoCode("KJFK");
    }
    
    @Test
    void shouldThrowExceptionForEmptyIcaoCode() {
        // Given
        String emptyIcaoCode = "";
        
        // When & Then
        assertThatThrownBy(() -> airportService.getAirportByIcaoCode(emptyIcaoCode)).isInstanceOf(InvalidIcaoCodeException.class).hasMessageContaining("ICAO code cannot be empty");
        
        verify(aviationDataClient, never()).getAirportByIcaoCode(anyString());
    }
    
    @Test
    void shouldThrowExceptionForNullIcaoCode() {
        // Given
        String nullIcaoCode = null;
        
        // When & Then
        assertThatThrownBy(() -> airportService.getAirportByIcaoCode(nullIcaoCode)).isInstanceOf(InvalidIcaoCodeException.class).hasMessageContaining("ICAO code cannot be empty");
        
        verify(aviationDataClient, never()).getAirportByIcaoCode(anyString());
    }
    
    @Test
    void shouldThrowExceptionForTooShortIcaoCode() {
        // Given
        String shortIcaoCode = "ABC";
        
        // When & Then
        assertThatThrownBy(() -> airportService.getAirportByIcaoCode(shortIcaoCode)).isInstanceOf(InvalidIcaoCodeException.class).hasMessageContaining("Invalid ICAO code format");
        
        verify(aviationDataClient, never()).getAirportByIcaoCode(anyString());
    }
    
    @Test
    void shouldThrowExceptionForTooLongIcaoCode() {
        // Given
        String longIcaoCode = "ABCDE";
        
        // When & Then
        assertThatThrownBy(() -> airportService.getAirportByIcaoCode(longIcaoCode)).isInstanceOf(InvalidIcaoCodeException.class).hasMessageContaining("Invalid ICAO code format");
        
        verify(aviationDataClient, never()).getAirportByIcaoCode(anyString());
    }
    
    @Test
    void shouldThrowExceptionForIcaoCodeWithNumbers() {
        // Given
        String icaoCodeWithNumbers = "K1FK";
        
        // When & Then
        assertThatThrownBy(() -> airportService.getAirportByIcaoCode(icaoCodeWithNumbers)).isInstanceOf(InvalidIcaoCodeException.class).hasMessageContaining("Invalid ICAO code format");
        
        verify(aviationDataClient, never()).getAirportByIcaoCode(anyString());
    }
    
    @Test
    void shouldThrowExceptionForIcaoCodeWithSpecialCharacters() {
        // Given
        String icaoCodeWithSpecialChars = "K@FK";
        
        // When & Then
        assertThatThrownBy(() -> airportService.getAirportByIcaoCode(icaoCodeWithSpecialChars)).isInstanceOf(InvalidIcaoCodeException.class).hasMessageContaining("Invalid ICAO code format");
        
        verify(aviationDataClient, never()).getAirportByIcaoCode(anyString());
    }
}


