package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.Repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobPersistenceService {

    private final JobRepository jobRepository;

    @Transactional
    public List<Job> saveNewJob(List<Job> jobs){
        List<Job> saved = new ArrayList<>();
        if (jobs.isEmpty()) {
            return saved;
        }

        for (Job job : jobs){
            job.setDedupKey(Job.buildDedupKey(job.getTitleJob(), job.getCompanyJob(), job.getLocation()));
        }

        // Duas consultas para o lote inteiro, em vez de duas por vaga.
        Set<String> knownUrls = new HashSet<>(jobRepository.findExistingSourceUrls(
                jobs.stream().map(Job::getSourceUrlJob).filter(Objects::nonNull).distinct().toList()));
        Set<String> knownKeys = new HashSet<>(jobRepository.findExistingDedupKeys(
                jobs.stream().map(Job::getDedupKey).distinct().toList()));

        for (Job job : jobs){
            boolean knownUrl = job.getSourceUrlJob() != null && knownUrls.contains(job.getSourceUrlJob());
            if (knownUrl || knownKeys.contains(job.getDedupKey())){
                continue;
            }
            jobRepository.save(job);
            saved.add(job);
            // Tambem cobre repeticoes dentro do proprio lote.
            knownKeys.add(job.getDedupKey());
            if (job.getSourceUrlJob() != null) {
                knownUrls.add(job.getSourceUrlJob());
            }
        }
        return saved;
    }
}
