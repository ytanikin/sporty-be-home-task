package com.sporty.behometask.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Internal DTO for mapping Aviation Weather API responses.
 * Isolated from the public API to allow provider changes without affecting consumers.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AviationApiAirportResponse {
    @JsonProperty("icaoId")
    private String icaoCode;
    
    @JsonProperty("iataId")
    private String iataCode;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("lat")
    private Double latitude;
    
    @JsonProperty("lon")
    private Double longitude;
    
    @JsonProperty("elev")
    private Integer elevation;
    
    public String getCity() {
        if (name != null && name.contains("/")) {
            return name.split("/")[0].trim();
        }
        return name;
    }
    
}


