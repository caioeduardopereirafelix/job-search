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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final JobMatchService jobMatchService;
    private final ApplicationEventPublisher eventPublisher;

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

        onTechnologiesLinked(user.getId(), linkTechnologies(user, request.technologies()));

        return toResponse(user, userTechnologyRepository.findByUserId(user.getId()));
    }

    @Transactional
    public UserResponse addTechnologies(UUID userId, List<String> technologyNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        onTechnologiesLinked(userId, linkTechnologies(user, technologyNames));

        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }


    @Transactional
    public List<String> addTechnologiesFromResume(UUID userId, List<String> technologyNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        List<String> linked = linkTechnologies(user, technologyNames);
        onTechnologiesLinked(userId, linked);
        return linked;
    }

    @Transactional
    public UserResponse removeTechnology(UUID userId, String technologyName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        UserTechnology link = userTechnologyRepository
                .findByUserIdAndTechnologyNameIgnoreCase(userId, technologyName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tecnologia nao associada a este usuario"));

        userTechnologyRepository.delete(link);
        jobMatchService.rematchUser(userId);

        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }


    private List<String> linkTechnologies(User user, List<String> technologyNames) {
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

        List<String> linked = new ArrayList<>();
        for (Technology technology : found) {
            int inserted = userTechnologyRepository.linkIfAbsent(user.getId(), technology.getId());
            if (inserted > 0) {
                linked.add(technology.getName());
            }
        }
        return linked;
    }


    private void onTechnologiesLinked(UUID userId, List<String> linkedTechnologies) {
        if (linkedTechnologies.isEmpty()) {
            return;
        }
        // Recalcula em vez de so completar: os matches que ja existiam guardam o score e
        // as tecnologias do perfil antigo, e ficariam desatualizados com o perfil maior.
        jobMatchService.rematchUser(userId);
        eventPublisher.publishEvent(new UserTechnologiesLinkedEvent(userId, linkedTechnologies));
    }

    private UserResponse toResponse(User user, List<UserTechnology> links) {
        List<String> technologyNames = links.stream()
                .map(ut -> ut.getTechnology().getName())
                .sorted(Comparator.naturalOrder())
                .toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), technologyNames, user.getCreatedAt());
    }
}