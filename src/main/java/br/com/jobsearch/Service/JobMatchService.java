package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.Domain.User;
import br.com.jobsearch.Dto.JobMatchResponse;
import br.com.jobsearch.Dto.PagedResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

            Set<UUID> alreadyMatched = new HashSet<>(jobMatchRepository.findJobIdsByUserId(user.getId()));
            for (Job job : jobs) {
                if (alreadyMatched.contains(job.getId())) {
                    continue;
                }
                createMatchIfApplicable(user, job, technologyNames);
            }
        }
    }

    @Transactional
    public void matchExistingJobsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        List<String> technologyNames = technologyNamesFor(userId);
        if (technologyNames.isEmpty()) {
            return;
        }

        Set<UUID> alreadyMatched = new HashSet<>(jobMatchRepository.findJobIdsByUserId(userId));
        for (Job job : jobRepository.findAll()) {
            if (alreadyMatched.contains(job.getId())) {
                continue;
            }
            createMatchIfApplicable(user, job, technologyNames);
        }
    }

    @Transactional
    public void rematchUser(UUID userId) {
        jobMatchRepository.deleteByUserId(userId);
        matchExistingJobsForUser(userId);
    }

    private List<String> technologyNamesFor(UUID userId) {
        return userTechnologyRepository.findByUserId(userId).stream()
                .map(ut -> ut.getTechnology().getName())
                .toList();
    }

    private void createMatchIfApplicable(User user, Job job, List<String> technologyNames) {
        List<String> matched = matchedTechnologies(job, technologyNames);
        if (matched.isEmpty()) {
            return;
        }

        jobMatchRepository.insertIfAbsent(
                user.getId(),
                job.getId(),
                (double) matched.size() / technologyNames.size(),
                matched.toArray(new String[0]),
                LocalDateTime.now());
    }

    private List<String> matchedTechnologies(Job job, List<String> technologyNames) {
        String haystack = (nullToEmpty(job.getTitleJob()) + " " + nullToEmpty(job.getDescriptionJob()))
                .toLowerCase(Locale.ROOT);

        return technologyNames.stream()
                .filter(name -> mentions(haystack, name))
                .toList();
    }


    static boolean mentions(String haystack, String technologyName) {
        return mentions(haystack, technologyName, true);
    }

    static boolean mentions(String haystack, String technologyName, boolean ignoreCase) {
        String regex = "(?<![\\p{L}\\p{N}])" + Pattern.quote(technologyName)
                + "(?![\\p{L}\\p{N}+#])";
        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        return Pattern.compile(regex, flags).matcher(haystack).find();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Transactional(readOnly = true)
    public List<JobMatchResponse> getMatchesForUser(UUID userId) {
        return getMatchesForUser(userId, 0, Integer.MAX_VALUE).content();
    }


    @Transactional(readOnly = true)
    public PagedResponse<JobMatchResponse> getMatchesForUser(UUID userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado");
        }

        Set<String> currentTechnologies = technologyNamesFor(userId).stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // Rede de seguranca: so mostra o match se ele ainda cita alguma tecnologia do perfil atual.
        List<JobMatchResponse> all = jobMatchRepository.findByUserIdOrderByScoreDesc(userId).stream()
                .filter(jm -> jm.getMatchedTechnologies() != null && jm.getMatchedTechnologies().stream()
                        .anyMatch(name -> currentTechnologies.contains(name.toLowerCase(Locale.ROOT))))
                .map(jm -> new JobMatchResponse(
                        jm.getJob().getId(),
                        jm.getJob().getTitleJob(),
                        jm.getJob().getCompanyJob(),
                        jm.getJob().getSourceUrlJob(),
                        jm.getScore(),
                        jm.getMatchedTechnologies(),
                        jm.getCreatedAt()))
                .toList();

        long from = Math.min((long) page * size, all.size());
        long to = Math.min(from + size, all.size());
        return PagedResponse.of(all.subList((int) from, (int) to), page, size, all.size());
    }
}
