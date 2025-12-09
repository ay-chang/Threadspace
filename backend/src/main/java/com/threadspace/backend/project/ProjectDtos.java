package com.threadspace.backend.project;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProjectDtos {

        public record ProjectCreateRequest(
                        String name,
                        String description,
                        String type,
                        UUID userId) {
        }

        public record ProjectResponse(
                        UUID id,
                        String name,
                        String description,
                        String type,
                        OffsetDateTime createdAt,
                        OffsetDateTime updatedAt,
                        UUID userId) {
        }
}
