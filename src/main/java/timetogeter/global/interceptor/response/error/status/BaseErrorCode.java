package timetogeter.global.interceptor.response.error.status;

import lombok.*;
import org.springframework.http.HttpStatus;
import timetogeter.global.interceptor.response.StatusCode;

//필요한 에러를 //[에러이름] 에러 란에 만들기
@Getter
@RequiredArgsConstructor
public enum BaseErrorCode implements StatusCode {
    //공통 에러
    INVALID_PARAMETER(1000,HttpStatus.BAD_REQUEST, "파라미터 값이 없습니다!"),
    INTERNAL_SERVER_ERROR(500,HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다!"),
    RESOURCE_NOT_FOUND(1002,HttpStatus.NOT_FOUND, "요청 url이 존재하지 않습니다!"),
    BAD_REQUEST(400 ,HttpStatus.BAD_REQUEST, "잘못된 URL 요청이에요. 요청 포맷을 확인해주세요"),

    //인증&인가 에러
    INVALID_LOGIN(400, HttpStatus.BAD_REQUEST, "예외처리 아이디 또는 비밀번호를 확인해주세요"),
    INVALID_AUTH(500, HttpStatus.INTERNAL_SERVER_ERROR, "인증 처리를 실패했어요"),
    INVALID_TOKEN(401, HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않아요"),
    INVALID_USER(403, HttpStatus.FORBIDDEN, "접근 권한이 없어요"),
    INVALID_PROVIDER(400, HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 플랫폼이에요"),
    INVALID_OAUTH_TOKEN(500, HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 토큰 발급을 실패했어요"),
    INVALID_DEK(400, HttpStatus.BAD_REQUEST, "DEK를 확인해주세요"),

    INVALID_ID_LENGTH(400, HttpStatus.BAD_REQUEST, "아이디는 1자 이상 20자 이하로 입력해주세요"),
    INVALID_ID_FORMAT(400, HttpStatus.BAD_REQUEST, "아이디 형식을 다시 확인해주세요"),
    INVALID_ID_DUP(400, HttpStatus.BAD_REQUEST, "다른 아이디를 사용해주세요"),
    INVALID_NICKNAME_LENGTH(400, HttpStatus.BAD_REQUEST, "닉네임은 1자 이상 20자 이하로 입력해주세요"),
    INVALID_NICKNAME_FORMAT(400, HttpStatus.BAD_REQUEST, "닉네임 형식을 다시 확인해주세요"),
    INVALID_EMAIL_FORMAT(400, HttpStatus.BAD_REQUEST, "이메일 형식을 다시 확인해주세요"),
    INVALID_PHONE_FORMAT(400, HttpStatus.BAD_REQUEST, "전화번호 형식을 다시 확인해주세요"),

    INVALID_TOKEN_REDIS(400, HttpStatus.BAD_REQUEST, "아이디에 해당하는 리프레시 토큰이 존재하지 않아요"),
    MISMATCH_TOKEN_REDIS(400, HttpStatus.BAD_REQUEST, "아이디에 해당하는 리프레시 토큰과 일치하지 않아요"),
    FAIL_LOGIN(401, HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 확인해주세요"),
    ACCOUNT_LOCKED(401, HttpStatus.UNAUTHORIZED, "계정이 잠겨 있습니다. 나중에 다시 시도하세요."),
    REDIS_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "세션 처리에 실패했어요"),

    SCHEDULE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "일정이 존재하지 않아요"),
    GROUP_NOT_FOUND(404, HttpStatus.NOT_FOUND, "그룹이 존재하지 않아요"),
    PLACE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "장소를 찾을 수 없어요"),
    INVALID_PLACE_NUM(400, HttpStatus.BAD_REQUEST, "장소 등록은 최대 5개까지 가능해요"),
    PLACE_RATING_NUM(400, HttpStatus.BAD_REQUEST, "장소 평점 범위는 1~5에요"),

    PROMISE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "약속을 찾을 수 없어요"),
    PROMISE_MANGER_FORBIDDEN(403, HttpStatus.FORBIDDEN, "약속장에 대한 접근 권한이 없어요"),
    PROMISE_LOOKUP_FORBIDDEN(403, HttpStatus.FORBIDDEN, "접근 권한이 없어요"),
    PROMISE_LOOKUP_ENC_USER_ID_REQUIRED(400, HttpStatus.BAD_REQUEST, "promisekey 조회를 위해 encUserId가 필요해요"),
    PROMISE_MEMBER_KEY_CONFLICT(409, HttpStatus.CONFLICT, "약속키 데이터가 일치하지 않아요"),
    LOOKUP_INVALID_FORMAT(400, HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않아요"),
    LOOKUP_VERSION_UNSUPPORTED(400, HttpStatus.BAD_REQUEST, "지원하지 않는 lookupVersion이에요"),
    LOOKUP_LEGACY_FALLBACK_DISABLED(400, HttpStatus.BAD_REQUEST, "legacy fallback이 종료되어 lookup 필드가 필요해요"),
    LOOKUP_NOT_FOUND(404, HttpStatus.NOT_FOUND, "lookup 매핑을 찾을 수 없어요"),
    LOOKUP_CONFLICT(409, HttpStatus.CONFLICT, "lookup 요청 상태가 충돌합니다."),
    VOTE_NUM_MAX(400, HttpStatus.BAD_REQUEST, "더이상 투표할 수 없어요"),
    PAGE_NOT_FOUND(400, HttpStatus.BAD_REQUEST, "총 페이지를 초과한 요청이에요"),
    INVALID_VOTE(403, HttpStatus.FORBIDDEN, "투표 취소권한이 없어요"),
    PLACE_NULL(400, HttpStatus.BAD_REQUEST, "장소등록에 필수정보가 누락됐어요"),
    INVALID_PLACE_NAME(400, HttpStatus.BAD_REQUEST, "장소등록은 1자 이상 30자 이내여야해요"),
    INVALID_PLACE_INFO(400, HttpStatus.BAD_REQUEST, "장소정보는 200자 이내여야해요"),
    PLACE_NOT_CONFIRM(409, HttpStatus.CONFLICT, "장소가 확정되지 않았어요"),
    PROMISE_TIME_RANGE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "약속에 시간범위 정보가 존재하지 않아요"),
    PROMISE_CHECK_NOT_FOUND(404, HttpStatus.NOT_FOUND, "임시 약속 테이블을 찾을 수 없어요"),
    PROMISE_KEY_NOT_FOUND(404, HttpStatus.NOT_FOUND, "약속 공유키 테이블을 찾을 수 없어요"),

    //그룹 에러
    GROUP_ID_NOTFOUND(400, HttpStatus.NOT_FOUND, "존재하지 않는 그룹 아이디 입니다."),
    GROUP_SHARE_KEY_INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "그룹 공유키 테이블 저장 로직 중 에러 발생했습니다."),
    GROUP_ID_DECRYPT_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "그룹 아이디 복호화 로직 중 에러 발생했습니다."),
    GROUP_KEY_DECRYPT_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "그룹키 복호화 로직 중 에러 발생했습니다."),
    NOT_GROUP_MANAGER_ERROR(500, HttpStatus.UNAUTHORIZED, "그룹 방장이 아니므로 그룹 정보를 수정할 수 없습니다."),
    NOT_GROUP_MEMBER_ERROR(403, HttpStatus.UNAUTHORIZED, "그룹 멤버가 아닙니다."),
    GROUP_INVITECODE_EXPIRED(403, HttpStatus.FORBIDDEN, "초대코드가 만료되었거나 유효하지 않습니다."),
    PROXY_USER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "프록시 사용자 매핑을 찾을 수 없어요"),
    GROUP_PROXY_USER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "해당 객체가 존재하지 않습니다."),
    GROUP_SHARE_KEY_NOT_FOUND(404,HttpStatus.NOT_FOUND, "해당 객체가 존재하지 않습니다." ),

    //서비스 내 유저 에러
    USER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    TIME_NOT_CONFIRM(404, HttpStatus.NOT_FOUND, "시간이 확정되지 않았어요");

    //===================================
    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
