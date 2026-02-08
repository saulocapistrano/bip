package br.com.bip.api.handler.exception;

import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ApiError buildError(HttpStatus status, String message, String path) {
        return new ApiError(
                OffsetDateTime.now(),
                status.value(),
                message,
                path
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        log.warn("Business error: {}", ex.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError body = buildError(status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Not found: {}", ex.getMessage());
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiError body = buildError(status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Requisição inválida");

        log.warn("Validation error: {}", message);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError body = buildError(status, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Data integrity violation", ex);
        HttpStatus status = HttpStatus.CONFLICT;
        ApiError body = buildError(status, "Violação de integridade de dados", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError body = buildError(status, "Erro interno no servidor", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
