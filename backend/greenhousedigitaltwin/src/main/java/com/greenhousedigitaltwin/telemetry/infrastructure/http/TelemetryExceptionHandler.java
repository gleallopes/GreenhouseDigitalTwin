package com.greenhousedigitaltwin.telemetry.infrastructure.http;

import com.greenhousedigitaltwin.telemetry.application.TelemetryValidationException;
import com.greenhousedigitaltwin.telemetry.domain.TelemetryIdentityConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.format.DateTimeParseException;

@RestControllerAdvice(assignableTypes = TelemetryController.class)
public class TelemetryExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TelemetryExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return invalidRequest();
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return invalidRequest();
    }

    @ExceptionHandler(DateTimeParseException.class)
    ResponseEntity<Object> invalidTimestamp() {
        return invalidRequest();
    }

    @ExceptionHandler(TelemetryValidationException.class)
    ResponseEntity<Object> rejected(TelemetryValidationException exception) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, exception.reason().name(), "Telemetry failed validation.");
    }

    @ExceptionHandler(TelemetryIdentityConflictException.class)
    ResponseEntity<Object> conflict() {
        return error(HttpStatus.CONFLICT, "IDENTITY_CONFLICT", "Telemetry identity conflicts with an existing event.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception exception) {
        log.error("Unexpected telemetry ingestion failure ({})", exception.getClass().getName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Telemetry ingestion failed.");
    }

    private ResponseEntity<Object> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request does not match the telemetry contract.");
    }

    private ResponseEntity<Object> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new TelemetryErrorResponse(code, message));
    }
}
