package timetogeter.global.interceptor.response.error.dto;

import lombok.*;
import org.springframework.http.ResponseEntity;
import timetogeter.global.interceptor.response.error.CustomException;
import timetogeter.global.interceptor.response.StatusCode;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder // 이거의 필요성이 있나?
public class ErrorResponse {
    private final String businessCode;
    private final int code;
    private final String message;
    private final String requestId;

    public static ResponseEntity<ErrorResponse> of(StatusCode code) {
        ErrorResponse res = new ErrorResponse(code.getBusinessCode(), code.getCode(), code.getMessage(), null);
        return new ResponseEntity<>(res, code.getHttpStatus());
    }

    public static ResponseEntity<ErrorResponse> of(CustomException exception) {
        ErrorResponse res = new ErrorResponse(
                exception.getStatus().getBusinessCode(),
                exception.getStatus().getCode(),
                exception.getMessage(),
                null
        );

        return new ResponseEntity<>(res, exception.getStatus().getHttpStatus());
    }

    public static ResponseEntity<ErrorResponse> of(StatusCode code, String requestId) {
        ErrorResponse res = new ErrorResponse(code.getBusinessCode(), code.getCode(), code.getMessage(), requestId);
        return new ResponseEntity<>(res, code.getHttpStatus());
    }

    public static ResponseEntity<ErrorResponse> of(CustomException exception, String requestId) {
        ErrorResponse res = new ErrorResponse(
                exception.getStatus().getBusinessCode(),
                exception.getStatus().getCode(),
                exception.getMessage(),
                requestId
        );
        return new ResponseEntity<>(res, exception.getStatus().getHttpStatus());
    }
}
