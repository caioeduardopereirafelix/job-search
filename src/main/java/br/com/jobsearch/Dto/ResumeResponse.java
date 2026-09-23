package br.com.jobsearch.Dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public record ResumeResponse(
        UUID id,
        String originalFileName,
        String extractText,
        LocalDateTime uploadAt,
        List<String> detectedTechnologies,
        List<String> addedTechnologies
) {
}
