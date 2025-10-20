package com.sporty.behometask.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.dto.ErrorResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration test for the Airport API.
 * Uses WireMock to simulate the external aviation API and verify the complete flow
 * including resilience patterns (retry, circuit breaker, rate limiter).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AirportControllerIntegrationTest {
    
    private static WireMockServer wireMockServer;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }
    
    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aviation.api.base-url", () -> wireMockServer.baseUrl() + "/api/v1");
        registry.add("resilience4j.retry.instances.aviationApi.max-attempts", () -> "2");
        registry.add("resilience4j.retry.instances.aviationApi.wait-duration", () -> "100ms");
    }
    
    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }
    
    @Test
    void shouldSuccessfullyRetrieveAirportInformation() {
        // Given
        String icaoCode = "KJFK";
        stubSuccessfulAirportResponse(icaoCode);
        
        // When
        ResponseEntity<AirportResponse> response = restTemplate.getForEntity("/api/v1/airports/{icaoCode}", AirportResponse.class, icaoCode);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIcaoCode()).isEqualTo("KJFK");
        assertThat(response.getBody().getName()).isEqualTo("John F Kennedy International Airport");
        assertThat(response.getBody().getCity()).isEqualTo("New York");
        assertThat(response.getBody().getCountry()).isEqualTo("United States");
        assertThat(response.getBody().getLocation()).isNotNull();
        assertThat(response.getBody().getLocation().getLatitude()).isEqualTo(40.6398);
        assertThat(response.getBody().getLocation().getLongitude()).isEqualTo(-73.7789);
        
        // Verify the external API was called exactly once
        verify(exactly(1), getRequestedFor(urlEqualTo("/api/v1/airports/KJFK")));
    }
    
    @Test
    void shouldReturnNotFoundWhenAirportDoesNotExist() {
        // Given
        String icaoCode = "XXXX";
        stubFor(get(urlEqualTo("/api/v1/airports/" + icaoCode)).willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Airport not found\"}")));
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/api/v1/airports/{icaoCode}", ErrorResponse.class, icaoCode);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("XXXX");
        assertThat(response.getBody().getTraceId()).isNotNull();
    }
    
    @Test
    void shouldReturnBadRequestForInvalidIcaoCode() {
        // Given
        String invalidIcaoCode = "123";
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/api/v1/airports/{icaoCode}", ErrorResponse.class, invalidIcaoCode);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("Invalid ICAO code format");
        
        // Verify the external API was never called
        verify(exactly(0), getRequestedFor(urlPathMatching("/api/v1/airports/.*")));
    }
    
    @Test
    void shouldRetryOnTransientFailures() {
        // Given
        String icaoCode = "EGLL";
        stubFor(get(urlEqualTo("/api/v1/airports/" + icaoCode)).inScenario("Retry").whenScenarioStateIs("Started").willReturn(aResponse().withStatus(500)
                        .withFixedDelay(100)).willSetStateTo("First Attempt Failed"));
        
        stubFor(get(urlEqualTo("/api/v1/airports/" + icaoCode)).inScenario("Retry").whenScenarioStateIs("First Attempt Failed")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(createAirportJsonResponse(icaoCode, "EGLL", "London Heathrow Airport", "London", "United Kingdom", 51.4700, -0.4543, "Europe/London", 25))));
        
        // When
        ResponseEntity<AirportResponse> response = restTemplate.getForEntity("/api/v1/airports/{icaoCode}", AirportResponse.class, icaoCode);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIcaoCode()).isEqualTo("EGLL");
        
        // Verify the external API was called twice (initial + 1 retry)
        verify(exactly(2), getRequestedFor(urlEqualTo("/api/v1/airports/EGLL")));
    }
    
    @Test
    void shouldHandleUpperAndLowerCaseIcaoCodes() {
        // Given
        String icaoCodeLowerCase = "kjfk";
        stubSuccessfulAirportResponse("KJFK");
        
        // When
        ResponseEntity<AirportResponse> response = restTemplate.getForEntity("/api/v1/airports/{icaoCode}", AirportResponse.class, icaoCodeLowerCase);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIcaoCode()).isEqualTo("KJFK");
        
        // Verify the external API was called with uppercase code
        verify(exactly(1), getRequestedFor(urlEqualTo("/api/v1/airports/KJFK")));
    }
    
    @Test
    void shouldHandleServiceUnavailableAfterAllRetries() {
        // Given
        String icaoCode = "LFPG";
        stubFor(get(urlEqualTo("/api/v1/airports/" + icaoCode)).willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Service unavailable\"}")));
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/api/v1/airports/{icaoCode}", ErrorResponse.class, icaoCode);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        
        verify(2, getRequestedFor(urlEqualTo("/api/v1/airports/LFPG")));
    }
    
    private void stubSuccessfulAirportResponse(String icaoCode) {
        stubFor(get(urlEqualTo("/api/v1/airports/" + icaoCode)).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody(createAirportJsonResponse(icaoCode, "KJFK", "John F Kennedy International Airport", "New York", "United States", 40.6398, -73.7789, "America/New_York", 13))));
    }
    
    private String createAirportJsonResponse(String icao, String iata, String name, String city, String country, double lat, double lon, String timezone, int elevation) {
        return String.format("{\"icao\":\"%s\",\"iata\":\"%s\",\"name\":\"%s\",\"city\":\"%s\",\"country\":\"%s\",\"lat\":%f,\"lon\":%f,\"timezone\":\"%s\",\"elevation\":%d}", icao, iata, name, city, country, lat, lon, timezone, elevation);
    }
}


