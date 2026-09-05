package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.UserTechnology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserTechnologyRepository extends JpaRepository<UserTechnology, UUID> {

    @Query("SELECT DISTINCT ut.technology.name FROM UserTechnology ut")
    List<String> findDistinctTechnologyNames();
}
