package com.threadspace.backend.project;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project createProject(String name, String description, String type, UUID userId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be empty");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Project type must not be empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        OffsetDateTime now = OffsetDateTime.now();

        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setType(type);
        project.setUserId(userId);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        return projectRepository.save(project);
    }
}
