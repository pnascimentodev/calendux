package pnascimento.dev.v1.exception;

import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;

/**
 * Standard DTO for error responses.
 */
@Getter
@Builder
public class ErrorResponse {
    private String message;
    private int status;
    private String error;
    private OffsetDateTime timestamp;
}
