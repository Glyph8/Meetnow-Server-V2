package timetogeter.context.group.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;

import java.util.UUID;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GroupExceptionHandler {
    @ExceptionHandler(GroupIdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupIdNotFoundException(
            GroupIdNotFoundException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupIdNotFoundException <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    @ExceptionHandler(GroupShareKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupShareKeyException(
            GroupShareKeyNotFoundException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupShareKeyException <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    @ExceptionHandler(GroupIdDecryptException.class)
    public ResponseEntity<ErrorResponse> handle_GroupIdDecryptException(
            GroupIdDecryptException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupIdDecryptException <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    @ExceptionHandler(GroupManagerMissException.class)
    public ResponseEntity<ErrorResponse> handle_GroupManagerMissException(
            GroupManagerMissException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupManagerMissException <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    @ExceptionHandler(GroupInviteCodeExpired.class)
    public ResponseEntity<ErrorResponse> handle_GroupInviteCodeExpired(
            GroupInviteCodeExpired e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupInviteCodeExpired <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupNotFoundException(
            GroupNotFoundException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupNotFoundException <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    @ExceptionHandler(GroupProxyUserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle_GroupProxyUserNotFoundException(
            GroupProxyUserNotFoundException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupProxyUserNotFoundException <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    @ExceptionHandler(GroupLookupValidationException.class)
    public ResponseEntity<ErrorResponse> handle_GroupLookupValidationException(
            GroupLookupValidationException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("GroupExceptionHandler.handle_GroupLookupValidationException <{}> {}", e.getMessage(), e);
        return withRequestId(ErrorResponse.of(e.getStatus(), resolveRequestId(request)), response);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }

    private ResponseEntity<ErrorResponse> withRequestId(
            ResponseEntity<ErrorResponse> responseEntity,
            HttpServletResponse response
    ) {
        ErrorResponse body = responseEntity.getBody();
        if (body != null && body.getRequestId() != null && !body.getRequestId().isBlank()) {
            response.setHeader("X-Request-Id", body.getRequestId());
        }
        return responseEntity;
    }
}
