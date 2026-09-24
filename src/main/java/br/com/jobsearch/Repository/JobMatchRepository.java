package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {
    /**
     * So os IDs das vagas ja combinadas com o usuario, numa consulta so - usado para filtrar
     * em memoria em vez de uma consulta "existsBy" por vaga (era o gargalo: milhares de idas
     * e vindas ao banco, uma por vaga, toda vez que alguem adiciona/remove uma tecnologia).
     */
    @Query("SELECT jm.job.id FROM JobMatch jm WHERE jm.user.id = :userId")
    List<UUID> findJobIdsByUserId(@Param("userId") UUID userId);

    List<JobMatch> findByUserIdOrderByScoreDesc(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM JobMatch jm WHERE jm.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
