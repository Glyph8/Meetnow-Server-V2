package timetogeter.global.interceptor.response.error;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;
import timetogeter.global.interceptor.response.StatusCode;

import java.util.Set;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Set<String> LOOKUP_CONTRACT_PATHS = Set.of(
            "/api/v1/group/invite1",
            "/api/v1/group/new2",
            "/api/v1/group/member/save",
            "/api/v1/group/leave1",
            "/api/v1/group/leave2",
            "/api/v1/group/edit1",
            "/api/v1/promise/create1",
            "/api/v1/promise/promisekey2",
            "/promise/create1",
            "/promise/promisekey2"
    );

    // 기존 컨텍스트별 ExceptionHandler에서 처리하지 못하는
    // 요청 본문 파싱/검증 예외를 ErrorResponse 포맷으로 통일한다.

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        StatusCode statusCode = resolveValidationStatusCode(request);
        String route = resolveRequestPath(request);
        String requestId = resolveRequestId(request);
        log.warn("GlobalExceptionHandler.handleMethodArgumentNotValidException requestId={}, route={}, code={}",
                requestId, route, statusCode.getCode(), e);
        return toObjectResponse(ErrorResponse.of(statusCode, requestId), requestId, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        StatusCode statusCode = resolveValidationStatusCode(request);
        String route = resolveRequestPath(request);
        String requestId = resolveRequestId(request);
        log.warn("GlobalExceptionHandler.handleHttpMessageNotReadableException requestId={}, route={}, code={}",
                requestId, route, statusCode.getCode(), e);
        return toObjectResponse(ErrorResponse.of(statusCode, requestId), requestId, request);
    }

    private StatusCode resolveValidationStatusCode(WebRequest request) {
        String path = resolveRequestPath(request);
        if (path == null) {
            return BaseErrorCode.INVALID_PARAMETER;
        }

        if (LOOKUP_CONTRACT_PATHS.contains(path)) {
            return BaseErrorCode.LOOKUP_INVALID_FORMAT;
        }

        return BaseErrorCode.INVALID_PARAMETER;
    }

    private String resolveRequestId(WebRequest request) {
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

    private String resolveRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }

    private ResponseEntity<Object> toObjectResponse(
            ResponseEntity<ErrorResponse> response,
            String requestId,
            WebRequest request
    ) {
        if (request instanceof ServletWebRequest servletWebRequest && requestId != null && !requestId.isBlank()) {
            if (servletWebRequest.getResponse() != null) {
                servletWebRequest.getResponse().setHeader("X-Request-Id", requestId);
            }
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

}
