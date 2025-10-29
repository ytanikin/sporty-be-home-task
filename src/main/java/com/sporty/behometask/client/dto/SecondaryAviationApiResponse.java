package com.sporty.behometask.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for mapping secondary aviation API (example.com) responses.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecondaryAviationApiResponse {
    @JsonProperty("code")
    private String icaoCode;
    
    @JsonProperty("iata")
    private String iataCode;
    
    @JsonProperty("airportName")
    private String name;
    
    @JsonProperty("cityName")
    private String city;
    
    @JsonProperty("countryName")
    private String country;
    
    @JsonProperty("lat")
    private Double latitude;
    
    @JsonProperty("lng")
    private Double longitude;
    
    @JsonProperty("elevationFt")
    private Integer elevation;
}

