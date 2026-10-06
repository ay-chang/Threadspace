package com.threadspace.backend.integration.stripe;

import java.util.UUID;

import com.threadspace.backend.integration.stripe.StripeDtos.StripeMetrics;
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
@RequestMapping("/projects/{projectId}/stripe")
public class StripeController {

    private static final Logger log = LoggerFactory.getLogger(StripeController.class);

    private final StripeService stripeService;

    @Value("${INTERNAL_SYNC_TOKEN}")
    private String internalSyncToken;

    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "30") int days,
            @RequestHeader(value = "x-internal-token", required = false) String token) {

        if (internalSyncToken == null || !internalSyncToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            StripeMetrics metrics = stripeService.getMetrics(projectId, days);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (StripeApiException e) {
            log.warn("Stripe API error for project {}: status={} code={} message={}",
                    projectId, e.getStatusCode(), e.getCode(), e.getStripeMessage());
            return switch (e.getStatusCode()) {
                case 401 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        "Stripe no longer accepts the stored key - it may have been rolled or deleted. "
                                + "Disconnect Stripe and reconnect with a new key.");
                case 403 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        "The stored key is missing a permission: "
                                + (e.getStripeMessage() != null ? e.getStripeMessage() : "access denied"));
                case 429 -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                        "Stripe is rate limiting requests. Try again in a minute.");
                default -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                        "Stripe returned an error (HTTP " + e.getStatusCode() + ").");
            };
        } catch (ResourceAccessException e) {
            log.warn("Could not reach Stripe for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Couldn't reach Stripe. Try again shortly.");
        } catch (Exception e) {
            log.error("Unexpected error fetching Stripe metrics for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to load Stripe metrics.");
        }
    }
}
