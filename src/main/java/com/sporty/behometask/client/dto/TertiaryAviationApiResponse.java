package com.sporty.behometask.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for mapping tertiary aviation API (example2.com) responses.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TertiaryAviationApiResponse {
    @JsonProperty("icao")
    private String icaoCode;
    
    @JsonProperty("iata_code")
    private String iataCode;
    
    @JsonProperty("full_name")
    private String name;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("country_code")
    private String country;
    
    @JsonProperty("latitude")
    private Double latitude;
    
    @JsonProperty("longitude")
    private Double longitude;
    
    @JsonProperty("elevation_meters")
    private Integer elevation;
}

