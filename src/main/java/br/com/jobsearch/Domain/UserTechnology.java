package br.com.jobsearch.Domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_technology")
public class UserTechnology {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "technology_id")
    private Technology technology;


}
