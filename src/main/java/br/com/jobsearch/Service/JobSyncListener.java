package br.com.jobsearch.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Busca vagas novas das tecnologias que o usuario acabou de adicionar, sem
 * esperar o proximo ciclo do scheduler. Roda depois do commit (para o vinculo
 * ja estar visivel) e em outra thread (a chamada a Adzuna e lenta).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobSyncListener {

    private final JobFetchService jobFetchService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTechnologiesLinked(UserTechnologiesLinkedEvent event) {
        for (String technology : event.technologyNames()) {
            try {
                jobFetchService.fetchAndPersist(technology);
            } catch (RuntimeException e) {
                log.warn("Falha ao buscar vagas de '{}' para o usuario {}: {}", technology, event.userId(), e.getMessage());
            }
        }
    }
}
