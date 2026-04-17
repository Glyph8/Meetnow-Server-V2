package timetogeter.context.group.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GroupExceptionHandler {
    @ExceptionHandler(GroupIdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupIdNotFoundException(GroupIdNotFoundException e) {
        log.error("GroupExceptionHandler.handle_GroupIdNotFoundException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(GroupShareKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupShareKeyException(GroupShareKeyNotFoundException e) {
        log.error("GroupExceptionHandler.handle_GroupShareKeyException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(GroupIdDecryptException.class)
    public ResponseEntity<ErrorResponse> handle_GroupIdDecryptException(GroupIdDecryptException e) {
        log.error("GroupExceptionHandler.handle_GroupIdDecryptException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(GroupManagerMissException.class)
    public ResponseEntity<ErrorResponse> handle_GroupManagerMissException(GroupManagerMissException e) {
        log.error("GroupExceptionHandler.handle_GroupManagerMissException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(GroupInviteCodeExpired.class)
    public ResponseEntity<ErrorResponse> handle_GroupInviteCodeExpired(GroupInviteCodeExpired e) {
        log.error("GroupExceptionHandler.handle_GroupInviteCodeExpired <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupNotFoundException(GroupNotFoundException e) {
        log.error("GroupExceptionHandler.handle_GroupNotFoundException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(GroupProxyUserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupProxyUserNotFoundException(GroupProxyUserNotFoundException e) {
        log.error("GroupExceptionHandler.handle_GroupProxyUserNotFoundException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }

    @ExceptionHandler(GroupLookupValidationException.class)
    public ResponseEntity<ErrorResponse> handle_GroupLookupValidationException(GroupLookupValidationException e) {
        log.error("GroupExceptionHandler.handle_GroupLookupValidationException <{}> {}", e.getMessage(), e);
        return ErrorResponse.of(e.getStatus());
    }
}
