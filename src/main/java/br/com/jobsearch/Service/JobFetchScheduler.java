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

    // fixedDelay curto por enquanto, só para testar mais rápido.
    // Trocar para @Scheduled(cron = "0 0 6 * * *") quando for para produção (1x por dia).
    @Scheduled(fixedDelay = 300000)
    public void fetchAndPersistJobs() {
        List<String> technologies = userTechnologyRepository.findDistinctTechnologyNames();

        // Fallback enquanto não há nenhum usuário cadastrado ainda (base vazia)
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
