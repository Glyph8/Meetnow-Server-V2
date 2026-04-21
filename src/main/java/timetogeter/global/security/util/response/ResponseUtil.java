package timetogeter.global.security.util.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;
import timetogeter.global.interceptor.response.StatusCode;

import java.io.IOException;
import java.util.UUID;

public class ResponseUtil {
    public static void handleException(StatusCode status, HttpServletResponse response) throws IOException {
        handleException(status, response, null);
    }

    public static void handleException(StatusCode status, HttpServletResponse response, String requestId) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String resolvedRequestId = resolveRequestId(requestId);

        ErrorResponse baseErrorResponse = ErrorResponse.of(status, resolvedRequestId).getBody();
        response.setStatus(status.getHttpStatus().value());
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("X-Request-Id", resolvedRequestId);

        response.getWriter().write(
                objectMapper.writeValueAsString(baseErrorResponse)
        );
    }

    private static String resolveRequestId(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
