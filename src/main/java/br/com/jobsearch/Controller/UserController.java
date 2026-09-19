package br.com.jobsearch.Controller;

import br.com.jobsearch.Dto.AddTechnologiesRequest;
import br.com.jobsearch.Dto.JobMatchResponse;
import br.com.jobsearch.Dto.UserRegistrationRequest;
import br.com.jobsearch.Dto.UserResponse;
import br.com.jobsearch.Service.JobMatchService;
import br.com.jobsearch.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JobMatchService jobMatchService;

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id, Authentication authentication) {
        requireOwner(id, authentication);
        return userService.getUser(id);
    }

    @PostMapping("/{id}/technologies")
    public UserResponse addTechnologies(@PathVariable UUID id, @Valid @RequestBody AddTechnologiesRequest request,
                                         Authentication authentication) {
        requireOwner(id, authentication);
        return userService.addTechnologies(id, request.technologies());
    }

    @GetMapping("/{id}/matches")
    public List<JobMatchResponse> getMatches(@PathVariable UUID id, Authentication authentication) {
        requireOwner(id, authentication);
        return jobMatchService.getMatchesForUser(id);
    }

    private void requireOwner(UUID id, Authentication authentication) {
        UUID authenticatedUserId = (UUID) authentication.getPrincipal();
        if (!authenticatedUserId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao tem acesso a este usuario");
        }
    }
}
