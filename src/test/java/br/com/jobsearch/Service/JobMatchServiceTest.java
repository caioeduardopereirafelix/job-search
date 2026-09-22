package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.Domain.JobMatch;
import br.com.jobsearch.Domain.Technology;
import br.com.jobsearch.Domain.User;
import br.com.jobsearch.Domain.UserTechnology;
import br.com.jobsearch.Dto.JobMatchResponse;
import br.com.jobsearch.Dto.PagedResponse;
import br.com.jobsearch.Repository.JobMatchRepository;
import br.com.jobsearch.Repository.JobRepository;
import br.com.jobsearch.Repository.UserRepository;
import br.com.jobsearch.Repository.UserTechnologyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserTechnologyRepository userTechnologyRepository;
    @Mock JobMatchRepository jobMatchRepository;
    @Mock JobRepository jobRepository;
    @InjectMocks JobMatchService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void mentionsMatchesWholeWordsOnly() {
        assertTrue(JobMatchService.mentions("vaga java senior", "Java"));
        assertFalse(JobMatchService.mentions("vaga javascript", "Java"));
        assertFalse(JobMatchService.mentions("marketing digital", "Git"));
        assertFalse(JobMatchService.mentions("programador c# ou c++", "C"));
        assertTrue(JobMatchService.mentions("experiencia com c, go e rust", "C"));
        assertTrue(JobMatchService.mentions("stack: spring boot, aws", "Spring Boot"));
    }

    @Test
    void getMatchesHidesJobsThatNoLongerMatchTheProfile() {
        when(userRepository.existsById(userId)).thenReturn(true);
        givenUserTechnologies("Java");

        JobMatch onlyAws = matchOf("Vaga AWS", List.of("AWS"));
        JobMatch javaAndAws = matchOf("Vaga Java", List.of("java", "AWS"));
        when(jobMatchRepository.findByUserIdOrderByScoreDesc(userId)).thenReturn(List.of(onlyAws, javaAndAws));

        List<JobMatchResponse> result = service.getMatchesForUser(userId);

        assertEquals(1, result.size());
        assertEquals("Vaga Java", result.get(0).titleJob());
    }

    @Test
    void getMatchesPaginatesAfterFilteringByCurrentProfile() {
        when(userRepository.existsById(userId)).thenReturn(true);
        givenUserTechnologies("Java");

        // 3 batem com o perfil atual, 1 nao bate (so AWS) e deve ficar de fora antes de paginar.
        JobMatch m1 = matchOf("Vaga 1", List.of("Java"));
        JobMatch m2 = matchOf("Vaga 2", List.of("Java"));
        JobMatch onlyAws = matchOf("Vaga AWS", List.of("AWS"));
        JobMatch m3 = matchOf("Vaga 3", List.of("Java"));
        when(jobMatchRepository.findByUserIdOrderByScoreDesc(userId)).thenReturn(List.of(m1, m2, onlyAws, m3));

        PagedResponse<JobMatchResponse> firstPage = service.getMatchesForUser(userId, 0, 2);
        assertEquals(List.of("Vaga 1", "Vaga 2"), firstPage.content().stream().map(JobMatchResponse::titleJob).toList());
        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());

        PagedResponse<JobMatchResponse> secondPage = service.getMatchesForUser(userId, 1, 2);
        assertEquals(List.of("Vaga 3"), secondPage.content().stream().map(JobMatchResponse::titleJob).toList());
    }

    @Test
    void getMatchesReturnsEmptyContentPastTheLastPage() {
        when(userRepository.existsById(userId)).thenReturn(true);
        givenUserTechnologies("Java");
        when(jobMatchRepository.findByUserIdOrderByScoreDesc(userId)).thenReturn(List.of(matchOf("Vaga 1", List.of("Java"))));

        PagedResponse<JobMatchResponse> beyondLastPage = service.getMatchesForUser(userId, 5, 10);

        assertEquals(List.of(), beyondLastPage.content());
        assertEquals(1, beyondLastPage.totalElements());
    }

    @Test
    void rematchUserDeletesOldMatchesBeforeMatchingAgain() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        givenUserTechnologies("React");
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitleJob("Dev React");
        when(jobRepository.findAll()).thenReturn(List.of(job));

        service.rematchUser(userId);

        InOrder order = inOrder(jobMatchRepository);
        order.verify(jobMatchRepository).deleteByUserId(userId);
        order.verify(jobMatchRepository).insertIfAbsent(any(), eq(job.getId()), anyDouble(), any(), any());
    }

    @Test
    void jobWithNoneOfTheUserTechnologiesIsNotMatched() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        givenUserTechnologies("Java", "Spring Boot");
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitleJob("Designer");
        job.setDescriptionJob("Figma e Photoshop");
        when(jobRepository.findAll()).thenReturn(List.of(job));

        service.matchExistingJobsForUser(userId);

        verify(jobMatchRepository, never()).insertIfAbsent(any(), any(), anyDouble(), any(), any());
    }

    private void givenUserTechnologies(String... names) {
        List<UserTechnology> links = new java.util.ArrayList<>();
        for (String name : names) {
            Technology technology = mock(Technology.class);
            when(technology.getName()).thenReturn(name);
            UserTechnology link = new UserTechnology();
            link.setTechnology(technology);
            links.add(link);
        }
        when(userTechnologyRepository.findByUserId(userId)).thenReturn(links);
    }

    private JobMatch matchOf(String title, List<String> matched) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitleJob(title);
        JobMatch match = new JobMatch();
        match.setJob(job);
        match.setScore(1.0);
        match.setMatchedTechnologies(matched);
        return match;
    }
}
