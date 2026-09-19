package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {
    boolean existsByUserIdAndJobId(UUID id, UUID id1);

    List<JobMatch> findByUserIdOrderByScoreDesc(UUID userId);
}
