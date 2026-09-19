package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.Domain.JobMatch;
import br.com.jobsearch.Domain.User;
import br.com.jobsearch.Dto.JobMatchResponse;
import br.com.jobsearch.Repository.JobMatchRepository;
import br.com.jobsearch.Repository.JobRepository;
import br.com.jobsearch.Repository.UserRepository;
import br.com.jobsearch.Repository.UserTechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final UserRepository userRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final JobMatchRepository jobMatchRepository;
    private final JobRepository jobRepository;

    @Transactional
    public void matchNewJobs(List<Job> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        for (User user : userRepository.findAll()) {
            List<String> technologyNames = technologyNamesFor(user.getId());
            if (technologyNames.isEmpty()) {
                continue;
            }

            for (Job job : jobs) {
                createMatchIfApplicable(user, job, technologyNames);
            }
        }
    }

    /**
     * Roda o match do usuario contra vagas que ja estao no banco - usado
     * quando o usuario se cadastra ou muda as tecnologias, pra nao precisar
     * esperar o proximo ciclo do scheduler trazer uma vaga nova.
     */
    @Transactional
    public void matchExistingJobsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        List<String> technologyNames = technologyNamesFor(userId);
        if (technologyNames.isEmpty()) {
            return;
        }

        for (Job job : jobRepository.findAll()) {
            createMatchIfApplicable(user, job, technologyNames);
        }
    }

    private List<String> technologyNamesFor(UUID userId) {
        return userTechnologyRepository.findByUserId(userId).stream()
                .map(ut -> ut.getTechnology().getName())
                .toList();
    }

    private void createMatchIfApplicable(User user, Job job, List<String> technologyNames) {
        if (jobMatchRepository.existsByUserIdAndJobId(user.getId(), job.getId())) {
            return;
        }

        List<String> matched = matchedTechnologies(job, technologyNames);
        if (matched.isEmpty()) {
            return;
        }

        JobMatch jobMatch = new JobMatch();
        jobMatch.setUser(user);
        jobMatch.setJob(job);
        jobMatch.setScore((double) matched.size() / technologyNames.size());
        jobMatch.setMatchedTechnologies(matched);
        jobMatch.setCreatedAt(LocalDateTime.now());
        jobMatchRepository.save(jobMatch);
    }

    private List<String> matchedTechnologies(Job job, List<String> technologyNames) {
        String haystack = (nullToEmpty(job.getTitleJob()) + " " + nullToEmpty(job.getDescriptionJob()))
                .toLowerCase(Locale.ROOT);

        return technologyNames.stream()
                .filter(name -> haystack.contains(name.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Transactional(readOnly = true)
    public List<JobMatchResponse> getMatchesForUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado");
        }

        return jobMatchRepository.findByUserIdOrderByScoreDesc(userId).stream()
                .map(jm -> new JobMatchResponse(
                        jm.getJob().getId(),
                        jm.getJob().getTitleJob(),
                        jm.getJob().getCompanyJob(),
                        jm.getJob().getSourceUrlJob(),
                        jm.getScore(),
                        jm.getMatchedTechnologies(),
                        jm.getCreatedAt()))
                .toList();
    }
}
