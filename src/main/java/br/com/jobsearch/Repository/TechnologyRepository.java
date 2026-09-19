package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.Technology;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnologyRepository extends JpaRepository<Technology, UUID> {

    @Query("SELECT t FROM Technology t WHERE UPPER(t.name) IN :namesUpper")
    List<Technology> findByNameUpperIn(@Param("namesUpper") List<String> namesUpper);
}
