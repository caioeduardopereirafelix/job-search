package br.com.jobsearch.Controller;

import br.com.jobsearch.Domain.Technology;
import br.com.jobsearch.Dto.TechnologyResponse;
import br.com.jobsearch.Repository.TechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/technologies")
@RequiredArgsConstructor
public class TechnologyController {

    private final TechnologyRepository technologyRepository;

    @GetMapping
    public List<TechnologyResponse> list() {
        return technologyRepository.findAll().stream()
                .sorted(Comparator.comparing(Technology::getName))
                .map(technology -> new TechnologyResponse(technology.getId(), technology.getName(), technology.getCategory()))
                .toList();
    }
}
