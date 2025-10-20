package com.sporty.behometask.controller;

import com.sporty.behometask.dto.AirportResponse;
import com.sporty.behometask.dto.ErrorResponse;
import com.sporty.behometask.service.AirportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for airport information endpoints.
 * Provides HTTP interface for retrieving airport data.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
@Tag(name = "Airports", description = "Airport information API")
public class AirportController {
    
    private final AirportService airportService;
    
    /**
     * Retrieves airport information by ICAO code.
     * 
     * @param icaoCode the 4-letter ICAO airport code (e.g., KJFK, EGLL, LFPG)
     * @return airport information
     */
    @GetMapping("/{icaoCode}")
    @Operation(summary = "Get airport by ICAO code", description = "Retrieves detailed information about an airport using its 4-letter ICAO code")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved airport information", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AirportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid ICAO code format", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Airport not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service unavailable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<AirportResponse> getAirportByIcaoCode(@Parameter(description = "4-letter ICAO airport code", example = "KJFK") @PathVariable String icaoCode) {
        log.info("GET /api/v1/airports/{} - Incoming request", icaoCode);
        AirportResponse response = airportService.getAirportByIcaoCode(icaoCode);
        log.info("GET /api/v1/airports/{} - Successfully returning response", icaoCode);
        return ResponseEntity.ok(response);
    }
}


