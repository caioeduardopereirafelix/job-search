package br.com.jobsearch.JobClient;

import br.com.jobsearch.Domain.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdzunaJobSourceClient implements JobSourceClient{

    private final RestClient restClient = RestClient.create();

    @Value("${ADZUNA_APP_ID}")
    private String appId;

    @Value("${ADZUNA_APP_KEY}")
    private String appKey;
    @Override
    public List<Job> fetchLatestJobs(String query, String location) {
        AdzunaResponse response = restClient.get()
                .uri("https://api.adzuna.com/v1/api/jobs/br/search/1" +
        "?app_id={appId}&app_key={appKey}&what={query}&where={location}" +
                "&results_per_page=20&content-type=application/json",
                appId, appKey, query, location)
                .retrieve()
                .body(AdzunaResponse.class);

        return response.results().stream()
                .map(this::toJob)
                .toList();
    }

    private Job toJob(AdzunaResult r){
        Job job = new Job();
        job.setTitleJob(r.title());
        job.setCompanyJob(r.company().displayName());
        job.setDescriptionJob(r.description());
        job.setSourceUrlJob(r.redirectUrl());
        job.setSourceNameJob("Adzuna");
        job.setLocation(r.location().location());
        job.setPostedAt(r.created());
        job.setFetchedAt(LocalDateTime.now());
        return job;
    }
}
