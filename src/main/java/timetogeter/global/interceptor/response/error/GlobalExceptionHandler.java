package timetogeter.global.interceptor.response.error;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;
import timetogeter.global.interceptor.response.StatusCode;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 기존 컨텍스트별 ExceptionHandler에서 처리하지 못하는
    // 요청 본문 파싱/검증 예외를 ErrorResponse 포맷으로 통일한다.

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        StatusCode statusCode = resolveValidationStatusCode(request);
        log.warn("GlobalExceptionHandler.handleMethodArgumentNotValidException requestId={}, route={}, code={}",
                resolveRequestId(request), request.getRequestURI(), statusCode.getCode(), e);
        return ErrorResponse.of(statusCode);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        StatusCode statusCode = resolveValidationStatusCode(request);
        log.warn("GlobalExceptionHandler.handleHttpMessageNotReadableException requestId={}, route={}, code={}",
                resolveRequestId(request), request.getRequestURI(), statusCode.getCode(), e);
        return ErrorResponse.of(statusCode);
    }

    private StatusCode resolveValidationStatusCode(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return BaseErrorCode.INVALID_PARAMETER;
        }

        if (path.equals("/api/v1/group/invite1")
                || path.equals("/api/v1/group/new2")
                || path.equals("/api/v1/group/member/save")
                || path.equals("/api/v1/group/leave1")
                || path.equals("/api/v1/group/leave2")
                || path.equals("/api/v1/group/edit1")) {
            return BaseErrorCode.LOOKUP_INVALID_FORMAT;
        }

        return BaseErrorCode.INVALID_PARAMETER;
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
        return "N/A";
    }

}
