package br.com.jobsearch.JobClient;

import br.com.jobsearch.Domain.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdzunaJobSourceClient implements JobSourceClient{

    private static final int RESULTS_PER_PAGE = 50;
    private static final int MAX_DAYS_OLD = 30;

    private final RestClient restClient = RestClient.create();

    @Value("${ADZUNA_APP_ID}")
    private String appId;

    @Value("${ADZUNA_APP_KEY}")
    private String appKey;
    @Override
    public List<Job> fetchLatestJobs(String query, String location, int page) {
        AdzunaResponse response;
        try {
            response = restClient.get()
                    .uri("https://api.adzuna.com/v1/api/jobs/br/search/{page}" +
                            "?app_id={appId}&app_key={appKey}&what={query}&where={location}" +
                            "&results_per_page=" + RESULTS_PER_PAGE +
                            "&sort_by=date&max_days_old=" + MAX_DAYS_OLD +
                            "&content-type=application/json",
                            page, appId, appKey, query, location)
                    .retrieve()
                    .body(AdzunaResponse.class);
        } catch (HttpClientErrorException e) {
            // Pagina alem do ultimo resultado: a Adzuna pode responder 4xx em vez de lista vazia.
            if (page > 1) {
                return List.of();
            }
            throw e;
        }

        if (response == null || response.results() == null) {
            return List.of();
        }

        return response.results().stream()
                .map(this::toJob)
                .toList();
    }

    static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("([?&])se=[^&]*&?", "$1").replaceAll("[?&]+$", "");
    }

    private Job toJob(AdzunaResult r){
        Job job = new Job();
        job.setTitleJob(r.title());
        job.setCompanyJob(r.company().displayName());
        job.setDescriptionJob(r.description());
        job.setSourceUrlJob(normalizeUrl(r.redirectUrl()));
        job.setSourceNameJob("Adzuna");
        job.setLocation(r.location().location());
        job.setPostedAt(r.created());
        job.setFetchedAt(LocalDateTime.now());
        return job;
    }
}
