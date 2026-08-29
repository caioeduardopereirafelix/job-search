package br.com.jobsearch.Respository;

import br.com.jobsearch.Domain.UserTechnology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTechnologyRepository extends JpaRepository<UserTechnology, UUID> {
    List<String> findDistinctTechnologyNames();
}
