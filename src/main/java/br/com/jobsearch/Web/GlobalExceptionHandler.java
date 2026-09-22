package br.com.jobsearch.Web;

import br.com.jobsearch.Dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Centraliza o formato das respostas de erro da API. Sem isto, uma excecao
 * nao prevista cai no /error padrao do Spring, cujo corpo varia conforme o
 * tipo de falha e pode nem incluir a mensagem.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        String message = ex.getReason() != null ? ex.getReason() : status.toString();
        return errorResponse(status, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return errorResponse(HttpStatus.BAD_REQUEST, "Parametros invalidos", request.getRequestURI(), details);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o tamanho maximo permitido", request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro nao tratado em {}", request.getRequestURI(), ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request.getRequestURI(), null);
    }

    // --- Excecoes que o ResponseEntityExceptionHandler ja intercepta (validacao de @Valid @RequestBody,
    // JSON malformado, parametro obrigatorio ausente, metodo HTTP nao suportado etc.) ---

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .toList();
        ApiErrorResponse body = ApiErrorResponse.of(400, "Bad Request", "Dados invalidos", path(request), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(400, "Bad Request", "Parametros invalidos", path(request), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(400, "Bad Request", "Corpo da requisicao ausente ou invalido", path(request), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(400, "Bad Request",
                "Parametro obrigatorio ausente: " + ex.getParameterName(), path(request), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(405, "Method Not Allowed", ex.getMessage(), path(request), null);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    private String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(HttpStatusCode status, String message, String path, List<String> fieldErrors) {
        HttpStatus httpStatus = HttpStatus.valueOf(status.value());
        ApiErrorResponse body = ApiErrorResponse.of(httpStatus.value(), httpStatus.getReasonPhrase(), message, path, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
