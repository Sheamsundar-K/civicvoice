package com.civicvoice.maps.controller;

import com.civicvoice.maps.dto.GeocodeResult;
import com.civicvoice.maps.dto.MapsLinkResponse;
import com.civicvoice.maps.service.GoogleMapsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Maps API controller.
 *
 * Frontend flow:
 *  1. User picks a location on the map widget (frontend uses Maps JS SDK with its own key)
 *  2. Frontend sends lat/lng to POST /api/v1/maps/reverse-geocode
 *  3. Backend resolves address components (ward, city, state) and returns them
 *  4. Frontend calls GET /api/v1/maps/links to get the embed/share URLs for display
 *  5. Frontend renders the embed iframe using the returned embedUrl
 *
 * This keeps the Geocoding API key server-side while the Maps JS API key
 * (for the interactive picker widget) can be a browser-restricted key.
 */
@RestController
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
@Validated
@Tag(name = "Maps", description = "Google Maps geocoding and link generation")
public class MapsController {

    private final GoogleMapsService googleMapsService;

    // ─── Reverse Geocode ─────────────────────────────────────────────────────

    @Operation(
        summary = "Reverse geocode coordinates to address",
        description = """
            Convert latitude/longitude to a structured address.
            Called after the user drops a pin on the interactive map widget.
            Returns city, ward, state, and formatted address pre-filled for the report form.
            """
    )
    @GetMapping("/reverse-geocode")
    public ResponseEntity<GeocodeResult> reverseGeocode(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng) {

        return ResponseEntity.ok(googleMapsService.reverseGeocode(lat, lng));
    }

    // ─── Forward Geocode ─────────────────────────────────────────────────────

    @Operation(
        summary = "Forward geocode an address to coordinates",
        description = "Convert a text address to lat/lng. Useful for searching locations by name."
    )
    @GetMapping("/geocode")
    public ResponseEntity<GeocodeResult> forwardGeocode(
            @RequestParam @NotBlank String address) {

        return ResponseEntity.ok(googleMapsService.forwardGeocode(address));
    }

    // ─── Maps Links ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Get Google Maps URLs for an issue location",
        description = """
            Returns a bundle of pre-built URLs for the frontend:
            - **embedUrl**: Use as `<iframe src="...">` to show the issue pin on an embedded map
            - **directionsUrl**: Opens Google Maps navigation to the issue
            - **streetViewUrl**: Opens Street View at the issue location
            - **shareUrl**: Public shareable link (no API key)
            - **staticThumbnailUrl**: Static map image for issue card previews
            
            The API key is never returned raw — it is embedded only inside the embedUrl
            which must be used in an iframe (not extractable via JS due to same-origin policy).
            """
    )
    @GetMapping("/links")
    public ResponseEntity<MapsLinkResponse> getMapsLinks(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng,
            @RequestParam(defaultValue = "Civic Issue") String label) {

        return ResponseEntity.ok(googleMapsService.buildMapsLinks(lat, lng, label));
    }
}
