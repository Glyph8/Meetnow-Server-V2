package timetogeter.global.security.exception.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import timetogeter.context.auth.exception.InvalidJwtException;
import timetogeter.global.interceptor.response.error.CustomException;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;

import java.io.IOException;

import static timetogeter.global.security.util.response.ResponseUtil.handleException;

@Component
@Slf4j
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(req, res);
        }catch (InvalidJwtException e){
            log.error("[ERROR] {}", e.getMessage());
            handleException(e.getStatus(), res, resolveRequestId(req));
        }catch (CustomException e){
            log.error("[ERROR] {}", e.getMessage(), e);
            handleException(e.getStatus(), res, resolveRequestId(req));
        }catch (JwtException | AuthenticationException e){
            log.error("[ERROR] {}", e.getMessage(), e);
            handleException(BaseErrorCode.INVALID_TOKEN, res, resolveRequestId(req));
        }catch (AccessDeniedException e){
            log.error("[ERROR] {}", e.getMessage(), e);
            handleException(BaseErrorCode.INVALID_USER, res, resolveRequestId(req));
        }catch (Exception e){
            Throwable rootCause = unwrapCause(e);
            if (rootCause instanceof InvalidJwtException invalidJwtException) {
                log.error("[ERROR] {}", invalidJwtException.getMessage(), invalidJwtException);
                handleException(invalidJwtException.getStatus(), res, resolveRequestId(req));
                return;
            }
            if (rootCause instanceof CustomException customException) {
                log.error("[ERROR] {}", customException.getMessage(), customException);
                handleException(customException.getStatus(), res, resolveRequestId(req));
                return;
            }
            if (rootCause instanceof JwtException || rootCause instanceof AuthenticationException) {
                log.error("[ERROR] {}", rootCause.getMessage(), rootCause);
                handleException(BaseErrorCode.INVALID_TOKEN, res, resolveRequestId(req));
                return;
            }
            if (rootCause instanceof AccessDeniedException) {
                log.error("[ERROR] {}", rootCause.getMessage(), rootCause);
                handleException(BaseErrorCode.INVALID_USER, res, resolveRequestId(req));
                return;
            }
            log.error("[ERROR] 알 수 없는 서버오류입니다 : {}", rootCause.getMessage(), rootCause);
            handleException(BaseErrorCode.INTERNAL_SERVER_ERROR, res, resolveRequestId(req));
        }
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
        return null;
    }

    private Throwable unwrapCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
