package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TechnologyRepository extends JpaRepository<Technology, UUID> {
}
