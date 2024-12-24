package org.example.exception;

import org.example.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<MessageResponse> notAuthorizedException(NotAuthorizedException e) {
        return new ResponseEntity<>(MessageResponse.builder().message(e.getMessage()).build(), UNAUTHORIZED);
    }
}
