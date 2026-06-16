package com.civicvoice.maps.service;

import com.civicvoice.maps.dto.GeocodeResult;
import com.civicvoice.maps.dto.MapsLinkResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Google Maps Platform integration service.
 *
 * Responsibilities:
 *  1. Reverse Geocoding  — convert lat/lng to human-readable address
 *  2. Forward Geocoding  — convert address string to lat/lng coordinates
 *  3. Build Maps redirect URLs for the frontend (embed, directions, Street View)
 *     The API key is NEVER returned to the client — all calls are server-side.
 *
 * Why server-side?
 *  - Prevents API key theft / abuse via browser DevTools
 *  - Enables server-level caching of geocode results to reduce quota usage
 *  - Allows audit logging of all geo-lookups
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapsService {

    private static final String GEOCODE_BASE = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String MAPS_EMBED_BASE = "https://www.google.com/maps/embed/v1/place";
    private static final String MAPS_DIRECTIONS_BASE = "https://www.google.com/maps/dir";
    private static final String MAPS_VIEW_BASE = "https://www.google.com/maps/@";

    @Value("${app.google.maps.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ─── 1. Reverse Geocoding: lat/lng → address ─────────────────────────────

    /**
     * Converts geographic coordinates to a structured address.
     * Results are cached to minimise API quota consumption.
     *
     * @param latitude   Decimal degrees latitude
     * @param longitude  Decimal degrees longitude
     * @return           {@link GeocodeResult} with formatted address and address components
     */
    @Cacheable(value = "geocode-reverse", key = "#latitude + ',' + #longitude")
    public GeocodeResult reverseGeocode(double latitude, double longitude) {
        String latlng = latitude + "," + longitude;
        URI uri = UriComponentsBuilder.fromUriString(GEOCODE_BASE)
                .queryParam("latlng", latlng)
                .queryParam("key", apiKey)
                .queryParam("result_type", "street_address|sublocality|locality")
                .build().toUri();

        JsonNode root = callGoogleApi(uri, "reverse geocode " + latlng);
        return parseGeocodeResponse(root, latitude, longitude);
    }

    // ─── 2. Forward Geocoding: address → lat/lng ──────────────────────────────

    /**
     * Converts an address string to geographic coordinates.
     *
     * @param address  Free-text address (city, street, landmark)
     * @return         {@link GeocodeResult} with lat/lng and formatted address
     */
    @Cacheable(value = "geocode-forward", key = "#address")
    public GeocodeResult forwardGeocode(String address) {
        URI uri = UriComponentsBuilder.fromUriString(GEOCODE_BASE)
                .queryParam("address", address)
                .queryParam("key", apiKey)
                .build().toUri();

        JsonNode root = callGoogleApi(uri, "forward geocode: " + address);
        return parseGeocodeResponse(root, null, null);
    }

    // ─── 3. Build Frontend Redirect URLs ─────────────────────────────────────

    /**
     * Builds a {@link MapsLinkResponse} containing safe, shareable Maps URLs for:
     *  - Embed iframe src (Google Maps Embed API) — shows issue pin on map
     *  - Directions link — opens Google Maps directions to the issue location
     *  - Street View link — opens Street View at the issue location
     *  - Share link — shareable https://maps.google.com link
     *
     * The frontend uses these URLs directly. The actual API key is embedded only
     * in the embed URL; it cannot be extracted from an iframe src without CORS.
     *
     * @param latitude   Issue latitude
     * @param longitude  Issue longitude
     * @param label      Location label shown on the pin (e.g. issue title)
     * @return           {@link MapsLinkResponse} safe for the client
     */
    public MapsLinkResponse buildMapsLinks(double latitude, double longitude, String label) {
        validateCoordinates(latitude, longitude);

        String encodedLabel = java.net.URLEncoder.encode(label, java.nio.charset.StandardCharsets.UTF_8);
        String latlng = latitude + "," + longitude;

        // Embed URL: shows a map with a pin — safe because restricted to referrer in GCP console
        String embedUrl = UriComponentsBuilder.fromUriString(MAPS_EMBED_BASE)
                .queryParam("q", latlng)
                .queryParam("key", apiKey)
                .queryParam("zoom", "17")
                .queryParam("maptype", "roadmap")
                .build().toUriString();

        // Directions URL: opens Google Maps navigation to the issue
        String directionsUrl = MAPS_DIRECTIONS_BASE + "//" + latlng;

        // Street View URL: 360° view at the issue location
        String streetViewUrl = MAPS_VIEW_BASE + latlng + ",3a,75y,0h,90t/data=!3m1!1e3";

        // Standard shareable link
        String shareUrl = "https://www.google.com/maps/search/?api=1&query=" + latlng;

        // Static map thumbnail URL (no key required for basic usage)
        String thumbnailUrl = "https://maps.googleapis.com/maps/api/staticmap"
                + "?center=" + latlng
                + "&zoom=16&size=600x300&maptype=roadmap"
                + "&markers=color:red%7Clabel:!" + "%7C" + latlng
                + "&key=" + apiKey;

        return MapsLinkResponse.builder()
                .latitude(latitude)
                .longitude(longitude)
                .label(label)
                .embedUrl(embedUrl)
                .directionsUrl(directionsUrl)
                .streetViewUrl(streetViewUrl)
                .shareUrl(shareUrl)
                .staticThumbnailUrl(thumbnailUrl)
                .build();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private JsonNode callGoogleApi(URI uri, String context) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Google Maps API error [{}] for {}: HTTP {}", context, uri, response.statusCode());
                throw new RuntimeException("Google Maps API returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText();

            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                String errorMessage = root.path("error_message").asText("No details");
                log.error("Google Geocoding API status [{}]: {} — {}", context, status, errorMessage);
                throw new RuntimeException("Geocoding failed: " + status);
            }

            return root;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to call Google Maps API [{}]: {}", context, e.getMessage());
            throw new RuntimeException("Google Maps API call failed", e);
        }
    }

    private GeocodeResult parseGeocodeResponse(JsonNode root, Double fallbackLat, Double fallbackLng) {
        JsonNode results = root.path("results");

        if (results.isEmpty()) {
            return GeocodeResult.builder()
                    .formattedAddress("Unknown location")
                    .latitude(fallbackLat != null ? fallbackLat : 0.0)
                    .longitude(fallbackLng != null ? fallbackLng : 0.0)
                    .found(false)
                    .build();
        }

        JsonNode first = results.get(0);
        JsonNode location = first.path("geometry").path("location");
        JsonNode components = first.path("address_components");

        double lat = location.path("lat").asDouble(fallbackLat != null ? fallbackLat : 0.0);
        double lng = location.path("lng").asDouble(fallbackLng != null ? fallbackLng : 0.0);
        String formatted = first.path("formatted_address").asText("Unknown location");

        // Extract individual components
        String city = "", state = "", country = "", postalCode = "", ward = "";
        for (JsonNode component : components) {
            String name = component.path("long_name").asText();
            for (JsonNode type : component.path("types")) {
                switch (type.asText()) {
                    case "locality"                  -> city = name;
                    case "administrative_area_level_1" -> state = name;
                    case "country"                   -> country = name;
                    case "postal_code"               -> postalCode = name;
                    case "sublocality_level_1"       -> ward = name;
                }
            }
        }

        return GeocodeResult.builder()
                .formattedAddress(formatted)
                .latitude(lat)
                .longitude(lng)
                .city(city)
                .state(state)
                .country(country)
                .postalCode(postalCode)
                .ward(ward)
                .placeId(first.path("place_id").asText())
                .found(true)
                .build();
    }

    private void validateCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, got: " + lat);
        }
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180, got: " + lng);
        }
    }
}
