package br.com.jobsearch.Service;

import br.com.jobsearch.Domain.Resume;
import br.com.jobsearch.Domain.User;
import br.com.jobsearch.Dto.ResumeFile;
import br.com.jobsearch.Dto.ResumeResponse;
import br.com.jobsearch.Repository.ResumeRepository;
import br.com.jobsearch.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeTechnologyExtractor technologyExtractor;
    private final UserService userService;

    @Value("${resume.storage-path}")
    private String storagePath;

    @Transactional
    public ResumeResponse upload(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        }

        String originalFileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        boolean isPdf = "application/pdf".equalsIgnoreCase(file.getContentType())
                || originalFileName.toLowerCase(Locale.ROOT).endsWith(".pdf");

        if (!isPdf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas arquivos PDF sao aceitos");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao ler o arquivo enviado");
        }

        String extractedText = extractText(content);
        Path storedPath = storeFile(userId, content);

        Resume resume = resumeRepository.findByUserId(userId).orElseGet(Resume::new);
        resume.setUser(user);
        resume.setOriginalFileName(originalFileName);
        resume.setExtractText(extractedText);
        resume.setFilePath(storedPath.toString());
        resume.setUploadAt(LocalDateTime.now());
        resumeRepository.save(resume);

        List<String> detected = technologyExtractor.extract(extractedText);
        List<String> added = userService.addTechnologiesFromResume(userId, detected);

        return toResponse(resume, detected, added);
    }

    @Transactional(readOnly = true)
    public ResumeResponse getResume(UUID userId) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario ainda nao enviou curriculo"));
        return toResponse(resume);
    }

    @Transactional
    public void deleteResume(UUID userId) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario ainda nao enviou curriculo"));

        try {
            Files.deleteIfExists(Path.of(resume.getFilePath()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao remover o arquivo armazenado");
        }

        resumeRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public ResumeFile download(UUID userId) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario ainda nao enviou curriculo"));

        try {
            byte[] content = Files.readAllBytes(Path.of(resume.getFilePath()));
            return new ResumeFile(content, resume.getOriginalFileName());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao ler o arquivo armazenado");
        }
    }

    private String extractText(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            return "";
        }
    }

    private Path storeFile(UUID userId, byte[] content) {
        try {
            Path directory = Path.of(storagePath);
            Files.createDirectories(directory);
            Path filePath = directory.resolve(userId + ".pdf");
            Files.write(filePath, content);
            return filePath;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao salvar o arquivo");
        }
    }

    private ResumeResponse toResponse(Resume resume) {
        return toResponse(resume, List.of(), List.of());
    }

    private ResumeResponse toResponse(Resume resume, List<String> detected, List<String> added) {
        return new ResumeResponse(resume.getId(), resume.getOriginalFileName(), resume.getExtractText(),
                resume.getUploadAt(), detected, added);
    }
}
