package zerowaste.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import zerowaste.backend.exception.classes.ConstraintException;

import java.time.Instant;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ErrorHttpResponse(String message, int status, String path, Instant timestamp) {}


    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorHttpResponse> handleValidation(ValidationException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ConstraintException.class)
    public ResponseEntity<ErrorHttpResponse> handleConstraint(ConstraintException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorHttpResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public  ResponseEntity<ErrorHttpResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, req.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorHttpResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }


    @ExceptionHandler(DisabledException.class)
    public  ResponseEntity<ErrorHttpResponse> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.FORBIDDEN, "Your account is disabled. Please verify your email.", req.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorHttpResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", req.getRequestURI());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public  ResponseEntity<ErrorHttpResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        return buildResponse((HttpStatus) ex.getStatusCode(), ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorHttpResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorHttpResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage() , req.getRequestURI());
    }

    private ResponseEntity<ErrorHttpResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorHttpResponse error = new ErrorHttpResponse(
                message,
                status.value(),
                path,
                Instant.now()
        );
        return ResponseEntity.status(status).body(error);
    }
}
