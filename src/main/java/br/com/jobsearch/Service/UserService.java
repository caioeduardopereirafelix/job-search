package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Technology;
import br.com.jobsearch.Domain.User;
import br.com.jobsearch.Domain.UserTechnology;
import br.com.jobsearch.Dto.UserRegistrationRequest;
import br.com.jobsearch.Dto.UserResponse;
import br.com.jobsearch.Enumerations.Roles;
import br.com.jobsearch.Repository.TechnologyRepository;
import br.com.jobsearch.Repository.UserRepository;
import br.com.jobsearch.Repository.UserTechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TechnologyRepository technologyRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Roles.JOB_USER);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        List<UserTechnology> links = linkTechnologies(user, request.technologies());

        return toResponse(user, links);
    }

    @Transactional
    public UserResponse addTechnologies(UUID userId, List<String> technologyNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        Set<String> already = userTechnologyRepository.findByUserId(userId).stream()
                .map(ut -> ut.getTechnology().getName().toUpperCase())
                .collect(Collectors.toSet());

        List<String> newOnes = technologyNames.stream()
                .filter(name -> !already.contains(name.trim().toUpperCase()))
                .toList();

        linkTechnologies(user, newOnes);

        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }

    private List<UserTechnology> linkTechnologies(User user, List<String> technologyNames) {
        if (technologyNames == null || technologyNames.isEmpty()) {
            return List.of();
        }

        List<String> namesUpper = technologyNames.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (namesUpper.isEmpty()) {
            return List.of();
        }

        List<Technology> found = technologyRepository.findByNameUpperIn(namesUpper);

        if (found.size() < namesUpper.size()) {
            Set<String> foundUpper = found.stream()
                    .map(t -> t.getName().toUpperCase())
                    .collect(Collectors.toSet());
            List<String> unknown = namesUpper.stream()
                    .filter(name -> !foundUpper.contains(name))
                    .toList();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tecnologias desconhecidas: " + unknown);
        }

        List<UserTechnology> links = found.stream().map(technology -> {
            UserTechnology userTechnology = new UserTechnology();
            userTechnology.setUser(user);
            userTechnology.setTechnology(technology);
            return userTechnology;
        }).toList();

        return userTechnologyRepository.saveAll(links);
    }

    private UserResponse toResponse(User user, List<UserTechnology> links) {
        List<String> technologyNames = links.stream()
                .map(ut -> ut.getTechnology().getName())
                .sorted(Comparator.naturalOrder())
                .toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), technologyNames, user.getCreatedAt());
    }
}
