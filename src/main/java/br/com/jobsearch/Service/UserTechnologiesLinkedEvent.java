package br.com.jobsearch.Service;

import java.util.List;
import java.util.UUID;

/** Publicado quando o usuario ganha tecnologias novas no perfil. */
public record UserTechnologiesLinkedEvent(UUID userId, List<String> technologyNames) {
}
