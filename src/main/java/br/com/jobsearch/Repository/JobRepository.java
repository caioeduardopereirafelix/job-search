package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    boolean existsBySourceUrlJob(String soruceUrl);
}
