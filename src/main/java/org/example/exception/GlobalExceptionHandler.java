package org.example.exception;

import org.example.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<MessageResponse> notAuthorizedExceptionHandler(NotAuthorizedException e) {
        return new ResponseEntity<>(MessageResponse.builder().message(e.getMessage()).build(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<MessageResponse> notFoundExceptionHandler(NotFoundException e) {
        return new ResponseEntity<>(MessageResponse.builder().message(e.getMessage()).build(), HttpStatus.NOT_FOUND);
    }
}
