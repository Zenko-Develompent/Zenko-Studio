package com.hackathon.edu.dto.progress;

import java.util.UUID;

public final class ProgressDTO {
    private ProgressDTO() {
    }

    public record ProgressResponse(
            UUID targetId,
            String targetType,
            int percent,
            boolean completed,
            long doneItems,
            long totalItems
    ) {
    }
}
