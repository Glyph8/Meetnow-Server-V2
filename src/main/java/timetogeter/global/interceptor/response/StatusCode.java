package timetogeter.global.interceptor.response;
import org.springframework.http.HttpStatus;


public interface StatusCode {
    int getCode();
    HttpStatus getHttpStatus();
    String getMessage();

    default String getBusinessCode() {
        if (this instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return String.valueOf(getCode());
    }
}
