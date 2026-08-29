package br.com.jobsearch.JobClient;

import br.com.jobsearch.Domain.Job;

import java.util.List;

public interface JobSourceClient {
    List<Job> fetchLatestJobs(String query, String location);
}
