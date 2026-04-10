package timetogeter.context.promise.application.dto.request.manage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(requiredProperties = {
        "promiseId",
        "lookupId",
        "lookupVersion"
})
public record GetPromiseRequest(
        @NotBlank String promiseId, //약속 아이디
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String lookupId, //조회 인덱스용 식별자(64-char hex)
        @NotNull Integer lookupVersion, //lookup 규칙 버전 (현재 1)
        String encUserId //호환 기간용 optional: 그룹키로 암호화한 사용자 아이디
) {
}
