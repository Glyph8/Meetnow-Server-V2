package timetogeter.global.interceptor.response.error;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import timetogeter.context.auth.exception.InvalidAuthException;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;
import timetogeter.global.interceptor.response.StatusCode;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    //로그인 예외 처리
    /*
    //예시
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        return handleExceptionInternal(errorCode);
    }
     */


    //그룹 예외 처리


    //===================================
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
//        log.warn("handleIllegalArgument", e);
//        StatusCode errorCode = BaseErrorCode.INVALID_PARAMETER;
//        return handleExceptionInternal(errorCode, e.getMessage());
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<?> handleAllException(Exception ex) {
//        log.warn("handleAllException", ex);
//        StatusCode errorCode = BaseErrorCode.INTERNAL_SERVER_ERROR;
//        return handleExceptionInternal(errorCode);
//    }
//
//    private ResponseEntity<?> handleExceptionInternal(StatusCode errorCode) {
//        return ResponseEntity.status(errorCode.getHttpStatus())
//                .body(makeErrorResponse(errorCode));
//    }
//
//    private ErrorResponse makeErrorResponse(StatusCode errorCode) {
//        return ErrorResponse.builder()
//                .code(errorCode.getCode())
//                .message(errorCode.getMessage())
//                .build();
//    }
//
//    private ResponseEntity<?> handleExceptionInternal(StatusCode errorCode, String message) {
//        return ResponseEntity.status(errorCode.getHttpStatus())
//                .body(makeErrorResponse(errorCode, message));
//    }
//
//    private ErrorResponse makeErrorResponse(StatusCode errorCode, String message) {
//        return ErrorResponse.builder()
//                .code(errorCode.getCode())
//                .message(message)
//                .build();
//    }
}
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
