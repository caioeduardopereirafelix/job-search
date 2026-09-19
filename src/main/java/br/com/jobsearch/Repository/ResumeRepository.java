package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findByUserId(UUID userId);
}
