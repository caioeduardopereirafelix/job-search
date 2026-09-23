package br.com.jobsearch.Service;

import java.util.List;
import java.util.UUID;


public record UserTechnologiesLinkedEvent(UUID userId, List<String> technologyNames) {
}
