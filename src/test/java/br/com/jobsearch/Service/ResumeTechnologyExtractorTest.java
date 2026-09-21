package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Technology;
import br.com.jobsearch.Repository.TechnologyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeTechnologyExtractorTest {

    @Mock TechnologyRepository technologyRepository;
    ResumeTechnologyExtractor extractor;

    @BeforeEach
    void setUp() {
        List<Technology> technologies = catalog(
                "Java", "JavaScript", "Spring Boot", "Spring", "PostgreSQL", "SQL", "Git", "GitHub",
                "C", "C#", "Julia", "REST", "Node.js", "React");
        lenient().when(technologyRepository.findAll()).thenReturn(technologies);
        extractor = new ResumeTechnologyExtractor(technologyRepository);
    }

    @Test
    void detectsCatalogTechnologiesIgnoringCase() {
        List<String> result = extractor.extract("Desenvolvedor JAVA com experiencia em postgresql e react.");

        assertEquals(List.of("Java", "PostgreSQL", "React"), result);
    }

    @Test
    void doesNotConfuseSimilarNames() {
        // "JavaScript" nao vale como "Java"; "PostgreSQL" nao vale como "SQL"; "GitHub" nao vale como "Git".
        List<String> result = extractor.extract("JavaScript, PostgreSQL e GitHub");

        assertEquals(List.of("GitHub", "JavaScript", "PostgreSQL"), result);
    }

    @Test
    void joinsNamesBrokenAcrossPdfLines() {
        assertEquals(List.of("Spring", "Spring Boot"), extractor.extract("Frameworks: Spring\n   Boot"));
    }

    @Test
    void ignoresAmbiguousNamesThatAreNotTechnologyMentions() {
        List<String> result = extractor.extract("Julia Souza. Habilitacao categoria C. Descansei o resto do dia; spring break.");

        assertEquals(List.of(), result);
    }

    @Test
    void caseSensitiveNamesNeedExactCase() {
        assertEquals(List.of(), extractor.extract("rest of the team, in spring"));
        assertEquals(List.of("REST", "Spring"), extractor.extract("APIs REST com Spring"));
    }

    @Test
    void handlesSymbolsInNames() {
        assertEquals(List.of("C#", "Node.js"), extractor.extract("Back-end em C# e Node.js, sem C++."));
    }

    @Test
    void blankTextDetectsNothing() {
        assertEquals(List.of(), extractor.extract("  "));
        assertEquals(List.of(), extractor.extract(null));
    }

    private static List<Technology> catalog(String... names) {
        return Arrays.stream(names).map(name -> {
            Technology technology = mock(Technology.class);
            lenient().when(technology.getName()).thenReturn(name);
            return technology;
        }).toList();
    }
}
