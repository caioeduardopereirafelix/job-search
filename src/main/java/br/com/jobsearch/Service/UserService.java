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
    private final JobMatchService jobMatchService;

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

        boolean linkedAny = linkTechnologies(user, request.technologies());

        if (linkedAny) {
            jobMatchService.matchExistingJobsForUser(user.getId());
        }

        return toResponse(user, userTechnologyRepository.findByUserId(user.getId()));
    }

    @Transactional
    public UserResponse addTechnologies(UUID userId, List<String> technologyNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        boolean linkedAny = linkTechnologies(user, technologyNames);

        if (linkedAny) {
            jobMatchService.matchExistingJobsForUser(userId);
        }

        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }

    @Transactional
    public UserResponse removeTechnology(UUID userId, String technologyName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        UserTechnology link = userTechnologyRepository
                .findByUserIdAndTechnologyNameIgnoreCase(userId, technologyName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tecnologia nao associada a este usuario"));

        userTechnologyRepository.delete(link);

        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        return toResponse(user, userTechnologyRepository.findByUserId(userId));
    }

    /**
     * Vincula as tecnologias informadas ao usuario. Cada vinculo e inserido
     * com ON CONFLICT DO NOTHING (via linkIfAbsent), entao chamar duas vezes
     * com a mesma tecnologia - inclusive em paralelo - nunca falha nem
     * duplica: so a que realmente inseriu retorna 1 linha afetada.
     *
     * @return true se pelo menos uma tecnologia nova foi de fato vinculada
     */
    private boolean linkTechnologies(User user, List<String> technologyNames) {
        if (technologyNames == null || technologyNames.isEmpty()) {
            return false;
        }

        List<String> namesUpper = technologyNames.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (namesUpper.isEmpty()) {
            return false;
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

        boolean linkedAny = false;
        for (Technology technology : found) {
            int inserted = userTechnologyRepository.linkIfAbsent(user.getId(), technology.getId());
            if (inserted > 0) {
                linkedAny = true;
            }
        }
        return linkedAny;
    }

    private UserResponse toResponse(User user, List<UserTechnology> links) {
        List<String> technologyNames = links.stream()
                .map(ut -> ut.getTechnology().getName())
                .sorted(Comparator.naturalOrder())
                .toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), technologyNames, user.getCreatedAt());
    }
}