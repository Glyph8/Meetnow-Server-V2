package timetogeter.context.promise.exception;

import timetogeter.global.interceptor.response.error.CustomException;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;

public class PromiseLookupForbiddenException extends CustomException {
    public PromiseLookupForbiddenException(BaseErrorCode status, String log) {
        super(status, log);
    }
}
