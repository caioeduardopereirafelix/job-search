package br.com.jobsearch.Web;

import br.com.jobsearch.Dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responseStatusExceptionKeepsStatusAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado");

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(ex, requestTo("/api/users/x"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Usuario nao encontrado", response.getBody().message());
        assertEquals("/api/users/x", response.getBody().path());
    }

    @Test
    void responseStatusExceptionWithoutReasonFallsBackToStatusText() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN);

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(ex, requestTo("/api/users/x"));

        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("403"));
    }

    @Test
    void constraintViolationListsEachInvalidField() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<SizeHolder>> violations = validator.validate(new SizeHolder(0));
        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<ApiErrorResponse> response = handler.handleConstraintViolation(ex, requestTo("/api/users/x/matches"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().fieldErrors().size());
        assertTrue(response.getBody().fieldErrors().get(0).contains("size"));
    }

    @Test
    void maxUploadSizeBecomes413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5_000_000L);

        ResponseEntity<ApiErrorResponse> response = handler.handleMaxUploadSize(ex, requestTo("/api/users/x/resume"));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
    }

    @Test
    void unexpectedExceptionIsHiddenBehind500WithGenericMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("detalhe interno sensivel"), requestTo("/api/users/x"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Erro interno inesperado", response.getBody().message());
    }

    private MockHttpServletRequest requestTo(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private record SizeHolder(@Min(1) int size) {
    }
}
