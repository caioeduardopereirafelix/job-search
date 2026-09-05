package br.com.jobsearch.Domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "technology")
public class Technology {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String category;


}
