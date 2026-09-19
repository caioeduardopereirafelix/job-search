package br.com.jobsearch.Dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String originalFileName,
        String extractText,
        LocalDateTime uploadAt
) {
}
