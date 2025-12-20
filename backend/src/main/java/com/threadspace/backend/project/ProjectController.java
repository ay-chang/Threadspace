package com.threadspace.backend.project;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.threadspace.backend.project.ProjectDtos.ProjectCreateRequest;
import static com.threadspace.backend.project.ProjectDtos.ProjectResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectsService;

    @Value("${INTERNAL_SYNC_TOKEN}")
    private String internalSyncToken;

    public ProjectController(ProjectService projectsService) {
        this.projectsService = projectsService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjectsForUser(
            @RequestParam("userId") UUID userId,
            @RequestHeader(name = "x-internal-token", required = false) String token) {
        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        List<ProjectResponse> responses = projectsService.getProjectsForUser(userId)
                .stream()
                .map(project -> new ProjectResponse(
                        project.getId(),
                        project.getName(),
                        project.getDescription(),
                        project.getType(),
                        project.getCreatedAt(),
                        project.getUpdatedAt(),
                        project.getUserId()))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody ProjectCreateRequest body,
            @RequestHeader(name = "x-internal-token", required = false) String token) {
        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Project project = projectsService.createProject(
                body.name(),
                body.description(),
                body.type(),
                body.userId());

        ProjectResponse response = new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getType(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getUserId());

        return ResponseEntity.ok(response);
    }
}
