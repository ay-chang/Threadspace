package com.threadspace.backend.integration.appstore;

import java.util.UUID;

import com.threadspace.backend.integration.appstore.AppStoreDtos.AppStoreMetrics;
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
@RequestMapping("/projects/{projectId}/appstore")
public class AppStoreController {

    private static final Logger log = LoggerFactory.getLogger(AppStoreController.class);

    private final AppStoreService appStoreService;

    @Value("${INTERNAL_SYNC_TOKEN}")
    private String internalSyncToken;

    public AppStoreController(AppStoreService appStoreService) {
        this.appStoreService = appStoreService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "14") int days,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (internalSyncToken == null || !internalSyncToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            AppStoreMetrics metrics = appStoreService.getMetrics(projectId, days);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AppStoreApiException e) {
            log.warn("App Store Connect API error for project {}: status={} code={} detail={}",
                    projectId, e.getStatusCode(), e.getCode(), e.getDetail());
            return switch (e.getStatusCode()) {
                case 401 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        "Apple no longer accepts the stored API key - it may have been revoked. "
                                + "Disconnect App Store Connect and reconnect with a new key.");
                case 403 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        "The stored API key can't download sales reports. It needs the Sales, Finance, or Admin role.");
                case 429 -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                        "Apple is rate limiting requests. Try again in a minute.");
                default -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                        "App Store Connect returned an error"
                                + (e.getDetail() != null ? ": " + e.getDetail() : " (HTTP " + e.getStatusCode() + ")."));
            };
        } catch (ResourceAccessException e) {
            log.warn("Could not reach App Store Connect for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Couldn't reach App Store Connect. Try again shortly.");
        } catch (Exception e) {
            log.error("Unexpected error fetching App Store Connect metrics for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to load App Store Connect metrics.");
        }
    }
}
