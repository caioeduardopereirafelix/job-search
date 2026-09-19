package br.com.jobsearch.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserRegistrationRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres") String password,
        List<String> technologies
) {
}
