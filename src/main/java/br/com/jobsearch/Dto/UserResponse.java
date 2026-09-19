package br.com.jobsearch.Dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        List<String> technologies,
        LocalDateTime createdAt
) {
}
