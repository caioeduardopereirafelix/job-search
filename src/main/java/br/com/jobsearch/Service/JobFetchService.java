package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.JobClient.JobSourceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Busca as vagas mais recentes de uma tecnologia, salva as novas e cruza com
 * o perfil de todos os usuarios. Usado pelo scheduler e pela busca sob demanda.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobFetchService {

    static final int MAX_PAGES = 3;

    private final JobSourceClient jobSourceClient;
    private final JobPersistenceService jobPersistenceService;
    private final JobMatchService jobMatchService;

    private final Map<String, Instant> lastFetched = new ConcurrentHashMap<>();

    // Scheduler e busca sob demanda podem rodar juntos; o lock evita que os dois
    // tentem inserir a mesma vaga ao mesmo tempo (dedup_key e unico).
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Percorre as paginas (da mais recente para a mais antiga) e para na
     * primeira que nao trouxer nenhuma vaga nova.
     *
     * @return quantas vagas novas foram salvas
     */
    public int fetchAndPersist(String technology) {
        lock.lock();
        try {
            int totalSaved = 0;
            for (int page = 1; page <= MAX_PAGES; page++) {
                List<Job> jobs = jobSourceClient.fetchLatestJobs(technology, "", page);
                if (jobs.isEmpty()) {
                    break;
                }

                List<Job> saved = jobPersistenceService.saveNewJob(jobs);
                jobMatchService.matchNewJobs(saved);
                totalSaved += saved.size();
                log.info("[{}] pagina {}: {} vagas novas de {} recebidas", technology, page, saved.size(), jobs.size());

                if (saved.isEmpty()) {
                    break;
                }
            }
            lastFetched.put(technology.toLowerCase(), Instant.now());
            return totalSaved;
        } finally {
            lock.unlock();
        }
    }

    /** Ordena para buscar primeiro as tecnologias que ficaram mais tempo sem atualizar. */
    public List<String> byOldestFetch(List<String> technologies) {
        return technologies.stream()
                .sorted(Comparator.comparing(t -> lastFetched.getOrDefault(t.toLowerCase(), Instant.EPOCH)))
                .toList();
    }
}
