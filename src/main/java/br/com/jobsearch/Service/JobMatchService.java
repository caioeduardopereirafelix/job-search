package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.Domain.JobMatch;
import br.com.jobsearch.Domain.User;
import br.com.jobsearch.Dto.JobMatchResponse;
import br.com.jobsearch.Repository.JobMatchRepository;
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

    @Transactional
    public void matchNewJobs(List<Job> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        for (User user : userRepository.findAll()) {
            List<String> technologyNames = userTechnologyRepository.findByUserId(user.getId()).stream()
                    .map(ut -> ut.getTechnology().getName())
                    .toList();

            if (technologyNames.isEmpty()) {
                continue;
            }

            for (Job job : jobs) {
                if (jobMatchRepository.existsByUserIdAndJobId(user.getId(), job.getId())) {
                    continue;
                }

                List<String> matched = matchedTechnologies(job, technologyNames);
                if (matched.isEmpty()) {
                    continue;
                }

                JobMatch jobMatch = new JobMatch();
                jobMatch.setUser(user);
                jobMatch.setJob(job);
                jobMatch.setScore((double) matched.size() / technologyNames.size());
                jobMatch.setMatchedTechnologies(matched);
                jobMatch.setCreatedAt(LocalDateTime.now());
                jobMatchRepository.save(jobMatch);
            }
        }
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
