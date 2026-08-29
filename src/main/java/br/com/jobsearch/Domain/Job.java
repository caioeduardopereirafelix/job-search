package br.com.jobsearch.Domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job")
@Getter
@Setter
public class Job {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;

    private String titleJob;

    private String companyJob;

    private String descriptionJob;

    private String sourceUrlJob;

    private String sourceNameJob;

    private String location;

    private LocalDateTime postedAt;

    private LocalDateTime fetchedAt;

}
