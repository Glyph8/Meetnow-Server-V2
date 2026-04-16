package timetogeter.context.group.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(requiredProperties = {
        "groupId",
        "lookupId",
        "lookupVersion"
})
public record LeavGroup1Request(
        @NotBlank String groupId,
        String encGroupId, //개인키로 암호화한 그룹 아이디(하위 호환 fallback용)
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String lookupId,
        @NotNull Integer lookupVersion
) {
}
