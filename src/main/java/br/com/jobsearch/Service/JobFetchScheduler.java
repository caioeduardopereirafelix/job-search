package br.com.jobsearch.Service;

import br.com.jobsearch.Repository.UserTechnologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobFetchScheduler {

    private final JobFetchService jobFetchService;
    private final UserTechnologyRepository userTechnologyRepository;

    @Scheduled(cron = "0 0 6 * * *")
    public void fetchAndPersistJobs() {
        List<String> technologies = userTechnologyRepository.findDistinctTechnologyNames();

        if (technologies.isEmpty()) {
            technologies = List.of("java");
        }

        for (String technology : jobFetchService.byOldestFetch(technologies)) {
            try {
                jobFetchService.fetchAndPersist(technology);
            } catch (RuntimeException e) {
                // Uma tecnologia que falha (ex.: limite da API) nao pode impedir as demais.
                log.warn("Falha ao buscar vagas de '{}': {}", technology, e.getMessage());
            }
        }
    }
}
