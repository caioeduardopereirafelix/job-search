package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.JobClient.JobSourceClient;
import br.com.jobsearch.Repository.UserTechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobFetchScheduler {

    private final JobSourceClient jobSourceClient;
    private final JobPersistenceService jobPersistenceService;
    private final UserTechnologyRepository userTechnologyRepository;

    // fixedDelay curto por enquanto, só para testar mais rápido.
    // Trocar para @Scheduled(cron = "0 0 6 * * *") quando for para produção (1x por dia).
    @Scheduled(fixedDelay = 300000)
    public void fetchAndPersistJobs() {
        List<String> technologies = userTechnologyRepository.findDistinctTechnologyNames();

        // Fallback enquanto não há nenhum usuário cadastrado ainda (base vazia)
        if (technologies.isEmpty()) {
            technologies = List.of("java");
        }

        for (String technology : technologies) {
            List<Job> jobs = jobSourceClient.fetchLatestJobs(technology, "");
            int saved = jobPersistenceService.saveNewJob(jobs);
            System.out.println("[" + technology + "] Vagas novas salvas: " + saved + " de " + jobs.size() + " recebidas");
        }
    }
}


