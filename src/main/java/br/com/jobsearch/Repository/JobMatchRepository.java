package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {
    boolean existsByUserIdAndJobId(UUID id, UUID id1);

    List<JobMatch> findByUserIdOrderByScoreDesc(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM JobMatch jm WHERE jm.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * Insere o match so se o par usuario+vaga ainda nao existir. ON CONFLICT
     * DO NOTHING e atomico no Postgres, evitando duplicatas quando o scheduler
     * e o cadastro/edicao de tecnologias rodam ao mesmo tempo.
     */
    @Modifying
    @Query(value = "INSERT INTO job_match (id, user_id, job_id, score, matched_technologies, created_at) "
            + "VALUES (gen_random_uuid(), :userId, :jobId, :score, :matched, :createdAt) "
            + "ON CONFLICT (user_id, job_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("jobId") UUID jobId,
                       @Param("score") double score, @Param("matched") String[] matched,
                       @Param("createdAt") LocalDateTime createdAt);
}
