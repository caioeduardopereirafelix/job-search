package br.com.jobsearch.Respository;

import br.com.jobsearch.Domain.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {
}
