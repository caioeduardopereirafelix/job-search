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

    @Column(name ="description_job", columnDefinition = "TEXT")
    private String descriptionJob;

    @Column(length = 2048)
    private String sourceUrlJob;

    private String sourceNameJob;

    private String location;

    private LocalDateTime postedAt;

    private LocalDateTime fetchedAt;

    /** Titulo + empresa + local normalizados; identifica a mesma vaga mesmo com URLs diferentes. */
    @Column(length = 800, unique = true)
    private String dedupKey;

    public static String buildDedupKey(String title, String company, String location) {
        return normalize(title) + "|" + normalize(company) + "|" + normalize(location);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

}
