package br.com.jobsearch.Dto;

import java.util.UUID;

public record LoginResponse(
        String token,
        UUID userId,
        String email,
        String name
) {
}
