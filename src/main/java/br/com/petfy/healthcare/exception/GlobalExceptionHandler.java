package br.com.petfy.healthcare.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PetfyHealthcareException.class)
    public ResponseEntity<ErrorResponse> handlePetfyHealthCareException(PetfyHealthcareException ex) {
        log.error("Handled business exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), ex.getCode(), ex.getHttpStatus().value(), LocalDateTime.now());
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse("Internal server error",500, 500, LocalDateTime.now());
        return ResponseEntity.status(500).body(error);
    }

}
