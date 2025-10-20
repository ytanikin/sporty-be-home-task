package com.sporty.behometask.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing airport information.
 * Designed to be provider-agnostic and contain only essential airport details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AirportResponse {
    private String icaoCode;
    private String iataCode;
    private String name;
    private String city;
    private String country;
    private LocationInfo location;
    private String timezone;
    private Integer elevation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
    }
}


