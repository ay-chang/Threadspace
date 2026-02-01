package com.threadspace.backend.integration.vercel;

import com.threadspace.backend.integration.vercel.VercelDtos.ProjectSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/vercel")
public class VercelController {

    private static final Logger log = LoggerFactory.getLogger(VercelController.class);

    private final VercelService vercelService;

    @Value("${INTERNAL_SYNC_TOKEN}")
    private String internalSyncToken;

    public VercelController(VercelService vercelService) {
        this.vercelService = vercelService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ProjectSummary> getVercelSummary(
            @PathVariable("projectId") UUID projectId,
            @RequestHeader(name = "x-internal-token", required = false) String token) {
        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ProjectSummary summary = vercelService.getProjectSummary(projectId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (VercelApiException e) {
            log.warn("Vercel API error for project {}: status={}, body={}", projectId, e.getStatusCode(),
                    e.getResponseBody());
            if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (e.getStatusCode() == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error fetching Vercel summary for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
