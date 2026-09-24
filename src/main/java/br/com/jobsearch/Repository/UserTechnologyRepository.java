package br.com.jobsearch.Repository;

import br.com.jobsearch.Domain.UserTechnology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserTechnologyRepository extends JpaRepository<UserTechnology, UUID> {

    @Query("SELECT DISTINCT ut.technology.name FROM UserTechnology ut")
    List<String> findDistinctTechnologyNames();

    List<UserTechnology> findByUserId(UUID userId);

    Optional<UserTechnology> findByUserIdAndTechnologyNameIgnoreCase(UUID userId, String technologyName);


    @Modifying
    @Query(value = "INSERT INTO user_technology (id, user_id, technology_id) "
            + "VALUES (gen_random_uuid(), :userId, :technologyId) "
            + "ON CONFLICT (user_id, technology_id) DO NOTHING", nativeQuery = true)
    int linkIfAbsent(@Param("userId") UUID userId, @Param("technologyId") UUID technologyId);
}