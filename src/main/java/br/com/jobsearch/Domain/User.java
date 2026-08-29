package br.com.jobsearch.Domain;

import br.com.jobsearch.Enumerations.Roles;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    private String password;

    @Enumerated(EnumType.STRING)
    private Roles roles;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<UserTechnology> technologies;

    @OneToOne(mappedBy = "user")
    private Resume resume;
}
