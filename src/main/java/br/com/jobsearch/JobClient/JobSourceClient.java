package br.com.jobsearch.JobClient;

import br.com.jobsearch.Domain.Job;

import java.util.List;

public interface JobSourceClient {
    /** @param page comeca em 1; paginas maiores trazem vagas mais antigas */
    List<Job> fetchLatestJobs(String query, String location, int page);

    default List<Job> fetchLatestJobs(String query, String location) {
        return fetchLatestJobs(query, location, 1);
    }
}
