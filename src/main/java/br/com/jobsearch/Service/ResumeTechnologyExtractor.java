package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Technology;
import br.com.jobsearch.Repository.TechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Descobre quais tecnologias do catalogo o texto de um curriculo menciona. */
@Component
@RequiredArgsConstructor
public class ResumeTechnologyExtractor {

    // Nomes que num curriculo quase sempre significam outra coisa
    // ("categoria C" da CNH, o nome proprio "Julia"). O usuario adiciona na mao.
    private static final Set<String> NEVER_DETECTED = Set.of("c", "julia");

    // Palavras comuns: so contam se escritas exatamente como no catalogo
    // ("REST", "Spring"), para nao casar com "rest", "spring" etc.
    private static final Set<String> CASE_SENSITIVE = Set.of(
            "rest", "spring", "swift", "oracle", "ruby", "dart", "lua", "perl", "assembly", "delphi");

    private final TechnologyRepository technologyRepository;

    public List<String> extract(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return List.of();
        }

        // PDFs quebram linha no meio de nomes como "Spring\nBoot".
        String text = resumeText.replaceAll("\\s+", " ");

        return technologyRepository.findAll().stream()
                .map(Technology::getName)
                .filter(name -> !NEVER_DETECTED.contains(name.toLowerCase(Locale.ROOT)))
                .filter(name -> JobMatchService.mentions(text, name, !CASE_SENSITIVE.contains(name.toLowerCase(Locale.ROOT))))
                .sorted()
                .toList();
    }
}
