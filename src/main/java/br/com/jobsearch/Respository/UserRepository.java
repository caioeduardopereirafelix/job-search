package br.com.jobsearch.Respository;

import br.com.jobsearch.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
