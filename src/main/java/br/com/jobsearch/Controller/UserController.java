package br.com.jobsearch.Controller;

import br.com.jobsearch.Dto.AddTechnologiesRequest;
import br.com.jobsearch.Dto.JobMatchResponse;
import br.com.jobsearch.Dto.PagedResponse;
import br.com.jobsearch.Dto.ResumeFile;
import br.com.jobsearch.Dto.ResumeResponse;
import br.com.jobsearch.Dto.UserRegistrationRequest;
import br.com.jobsearch.Dto.UserResponse;
import br.com.jobsearch.Service.JobMatchService;
import br.com.jobsearch.Service.ResumeService;
import br.com.jobsearch.Service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JobMatchService jobMatchService;
    private final ResumeService resumeService;

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

    @DeleteMapping("/{id}/technologies/{technologyName}")
    public UserResponse removeTechnology(@PathVariable UUID id, @PathVariable String technologyName,
                                          Authentication authentication) {
        requireOwner(id, authentication);
        return userService.removeTechnology(id, technologyName);
    }

    @GetMapping("/{id}/matches")
    public PagedResponse<JobMatchResponse> getMatches(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        requireOwner(id, authentication);
        return jobMatchService.getMatchesForUser(id, page, size);
    }

    @PostMapping(value = "/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeResponse uploadResume(@PathVariable UUID id, @RequestParam("file") MultipartFile file,
                                        Authentication authentication) {
        requireOwner(id, authentication);
        return resumeService.upload(id, file);
    }

    @GetMapping("/{id}/resume")
    public ResumeResponse getResume(@PathVariable UUID id, Authentication authentication) {
        requireOwner(id, authentication);
        return resumeService.getResume(id);
    }

    @GetMapping("/{id}/resume/download")
    public ResponseEntity<byte[]> downloadResume(@PathVariable UUID id, Authentication authentication) {
        requireOwner(id, authentication);
        ResumeFile resumeFile = resumeService.download(id);

        String encodedFileName = java.net.URLEncoder.encode(resumeFile.originalFileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"")
                .body(resumeFile.content());
    }

    private void requireOwner(UUID id, Authentication authentication) {
        UUID authenticatedUserId = (UUID) authentication.getPrincipal();
        if (!authenticatedUserId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao tem acesso a este usuario");
        }
    }
}