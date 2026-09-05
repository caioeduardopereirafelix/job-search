package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {
}
