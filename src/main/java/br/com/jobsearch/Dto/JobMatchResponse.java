package br.com.jobsearch.Dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record JobMatchResponse(
        UUID jobId,
        String titleJob,
        String companyJob,
        String sourceUrlJob,
        Double score,
        List<String> matchedTechnologies,
        LocalDateTime createdAt
) {
}
