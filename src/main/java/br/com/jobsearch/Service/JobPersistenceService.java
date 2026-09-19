package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.Repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobPersistenceService {

    private final JobRepository jobRepository;

    @Transactional
    public int saveNewJob(List<Job> jobs){
        int savedCount = 0;
        for (Job job : jobs){
            if (!jobRepository.existsBySourceUrlJob(job.getSourceUrlJob())){
                jobRepository.save(job);
                savedCount++;
            }
        }
        return savedCount;
    }
}
