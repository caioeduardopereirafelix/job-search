package br.com.jobsearch.Dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * detectedTechnologies e addedTechnologies so vem preenchido no upload:
 * o primeiro e tudo que o curriculo menciona; o segundo, o que entrou agora no perfil.
 */
public record ResumeResponse(
        UUID id,
        String originalFileName,
        String extractText,
        LocalDateTime uploadAt,
        List<String> detectedTechnologies,
        List<String> addedTechnologies
) {
}
