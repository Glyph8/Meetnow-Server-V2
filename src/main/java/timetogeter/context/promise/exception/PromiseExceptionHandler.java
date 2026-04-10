package timetogeter.context.promise.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class PromiseExceptionHandler {

   /* @ExceptionHandler(GroupIdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_PromiseIdNotFoundException(GroupIdNotFoundException e) {
        log.error("PromiseExceptionHandler.handle_PromiseIdNotFoundException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }*/

    @ExceptionHandler(PromiseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_PromiseNotFoundException(PromiseNotFoundException e) {
        log.error("PromiseExceptionHandler.handle_PromiseNotFoundException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(PromiseLookupValidationException.class)
    public ResponseEntity<ErrorResponse> handle_PromiseLookupValidationException(PromiseLookupValidationException e) {
        log.error("PromiseExceptionHandler.handle_PromiseLookupValidationException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(PromiseLookupForbiddenException.class)
    public ResponseEntity<ErrorResponse> handle_PromiseLookupForbiddenException(PromiseLookupForbiddenException e) {
        log.error("PromiseExceptionHandler.handle_PromiseLookupForbiddenException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(PromiseMemberKeyConflictException.class)
    public ResponseEntity<ErrorResponse> handle_PromiseMemberKeyConflictException(PromiseMemberKeyConflictException e) {
        log.error("PromiseExceptionHandler.handle_PromiseMemberKeyConflictException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }
}
