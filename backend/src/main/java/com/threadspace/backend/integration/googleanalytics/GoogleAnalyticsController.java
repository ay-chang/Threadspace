package com.threadspace.backend.integration.googleanalytics;

import java.util.UUID;

import com.threadspace.backend.integration.google.GoogleTokenException;
import com.threadspace.backend.integration.googleanalytics.GoogleAnalyticsDtos.GoogleAnalyticsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

@RestController
@RequestMapping("/projects/{projectId}/google-analytics")
public class GoogleAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(GoogleAnalyticsController.class);

    private final GoogleAnalyticsService service;

    @Value("${INTERNAL_SYNC_TOKEN}")
    private String internalSyncToken;

    public GoogleAnalyticsController(GoogleAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "28") int days,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (internalSyncToken == null || !internalSyncToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            GoogleAnalyticsMetrics metrics = service.getMetrics(projectId, days);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (GoogleTokenException e) {
            log.warn("Google token exchange failed for project {}: {}", projectId, e.getMessage());
            return e.isRejected()
                    ? ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            "Google no longer accepts the stored service account key - it may have been deleted "
                                    + "or disabled. Disconnect Google Analytics and reconnect with a new key.")
                    : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Couldn't reach Google. Try again shortly.");
        } catch (GoogleAnalyticsApiException e) {
            log.warn("Google Analytics API error for project {}: status={} reason={} message={}",
                    projectId, e.getStatusCode(), e.getReason(), e.getApiMessage());
            if (e.isApiDisabled()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        "The Google Analytics Data API has been disabled for this service account's project. "
                                + "Re-enable it in the Google Cloud console.");
            }
            return switch (e.getStatusCode()) {
                case 403 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        "The service account no longer has access to this GA4 property. Add it back under "
                                + "Admin -> Property access management with the Viewer role.");
                case 429 -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                        "Google Analytics is rate limiting this property. Try again in a minute.");
                default -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                        "Google Analytics returned an error"
                                + (e.getApiMessage() != null ? ": " + e.getApiMessage() : " (HTTP " + e.getStatusCode() + ")."));
            };
        } catch (ResourceAccessException e) {
            log.warn("Could not reach Google Analytics for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Couldn't reach Google Analytics. Try again shortly.");
        } catch (Exception e) {
            log.error("Unexpected error fetching Google Analytics metrics for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to load Google Analytics metrics.");
        }
    }
}
