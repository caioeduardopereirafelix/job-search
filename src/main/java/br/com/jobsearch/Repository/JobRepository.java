package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    boolean existsBySourceUrlJob(String soruceUrl);

    boolean existsByDedupKey(String dedupKey);

    @Query("SELECT j.sourceUrlJob FROM Job j WHERE j.sourceUrlJob IN :urls")
    List<String> findExistingSourceUrls(@Param("urls") Collection<String> urls);

    @Query("SELECT j.dedupKey FROM Job j WHERE j.dedupKey IN :keys")
    List<String> findExistingDedupKeys(@Param("keys") Collection<String> keys);
}
