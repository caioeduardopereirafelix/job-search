package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.Repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobPersistenceService {

    private final JobRepository jobRepository;

    @Transactional
    public List<Job> saveNewJob(List<Job> jobs){
        List<Job> saved = new ArrayList<>();
        for (Job job : jobs){
            job.setDedupKey(Job.buildDedupKey(job.getTitleJob(), job.getCompanyJob(), job.getLocation()));
            if (!jobRepository.existsBySourceUrlJob(job.getSourceUrlJob())
                    && !jobRepository.existsByDedupKey(job.getDedupKey())){
                jobRepository.save(job);
                saved.add(job);
            }
        }
        return saved;
    }
}
