package com.civicvoice.maps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Structured result from Google Geocoding API.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeocodeResult {
    private final double latitude;
    private final double longitude;
    private final String formattedAddress;
    private final String city;
    private final String state;
    private final String country;
    private final String postalCode;
    private final String ward;
    private final String placeId;
    private final boolean found;
}
