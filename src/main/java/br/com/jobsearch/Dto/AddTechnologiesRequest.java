package br.com.jobsearch.Dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddTechnologiesRequest(
        @NotEmpty(message = "Informe ao menos uma tecnologia") List<String> technologies
) {
}
