package com.civicvoice.maps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Safe Maps URL bundle returned to the frontend.
 * The embed URL contains the API key but is restricted to allowed referrers via GCP console.
 * The other URLs (directions, streetView, share) are key-free public links.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MapsLinkResponse {
    private final double latitude;
    private final double longitude;
    private final String label;

    /** Use as <iframe src="embedUrl" ...> in the frontend. */
    private final String embedUrl;

    /** Opens Google Maps turn-by-turn navigation to the issue. */
    private final String directionsUrl;

    /** Opens Google Street View at the issue location. */
    private final String streetViewUrl;

    /** Public shareable link – no key required. */
    private final String shareUrl;

    /** Static map image URL for thumbnail previews. */
    private final String staticThumbnailUrl;
}
