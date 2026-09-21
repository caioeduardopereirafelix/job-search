package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.JobClient.JobSourceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobFetchServiceTest {

    @Mock JobSourceClient client;
    @Mock JobPersistenceService persistence;
    @Mock JobMatchService matchService;
    @InjectMocks JobFetchService service;

    @Test
    void keepsPagingWhileNewJobsAppearAndStopsAtMaxPages() {
        List<Job> page = List.of(new Job());
        when(client.fetchLatestJobs(eq("java"), anyString(), anyInt())).thenReturn(page);
        when(persistence.saveNewJob(page)).thenReturn(page);

        int saved = service.fetchAndPersist("java");

        assertEquals(JobFetchService.MAX_PAGES, saved);
        verify(client, times(JobFetchService.MAX_PAGES)).fetchLatestJobs(eq("java"), anyString(), anyInt());
    }

    @Test
    void stopsAtFirstPageWithNothingNew() {
        List<Job> page = List.of(new Job());
        when(client.fetchLatestJobs("java", "", 1)).thenReturn(page);
        when(persistence.saveNewJob(page)).thenReturn(List.of());

        assertEquals(0, service.fetchAndPersist("java"));

        verify(client, never()).fetchLatestJobs("java", "", 2);
    }

    @Test
    void stopsWhenThePageIsEmpty() {
        when(client.fetchLatestJobs("java", "", 1)).thenReturn(List.of());

        assertEquals(0, service.fetchAndPersist("java"));

        verify(persistence, never()).saveNewJob(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void technologiesFetchedLongestAgoComeFirst() {
        when(client.fetchLatestJobs(anyString(), anyString(), anyInt())).thenReturn(List.of());
        service.fetchAndPersist("java");

        assertEquals(List.of("react", "java"), service.byOldestFetch(List.of("java", "react")));
    }
}
