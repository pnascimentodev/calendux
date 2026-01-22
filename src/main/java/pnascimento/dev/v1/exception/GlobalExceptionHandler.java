package pnascimento.dev.v1.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/**
 * Intercepts all exceptions and formats them as JSON
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles custom application exceptions
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .timestamp(OffsetDateTime.now())
                .build();

        return new ResponseEntity<>(response, ex.getStatus());
    }

    /**
     * Fallback for unexpected generic errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Log the actual error internally
        ex.printStackTrace();

        ErrorResponse response = ErrorResponse.builder()
                .message("An internal server error occurred. / Ocorreu um erro interno no servidor.")
                .status(500)
                .error("Internal Server Error")
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.internalServerError().body(response);
    }
}
