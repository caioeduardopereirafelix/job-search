package br.com.jobsearch.Domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "technology")
public class Technology {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;

    private String name;

    private String category;


}
