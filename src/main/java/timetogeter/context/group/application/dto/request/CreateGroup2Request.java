package timetogeter.context.group.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(requiredProperties = {
        "groupId",
        "encGroupId",
        "encencGroupMemberId",
        "encUserId",
        "encGroupKey",
        "lookupId",
        "lookupVersion"
})
public record CreateGroup2Request(
        @NotBlank String groupId, //그룹 아이디
        @NotBlank String encGroupId, //개인키로 암호화한 그룹 아이디
        @NotBlank String encencGroupMemberId, //개인키로 암호화한 (그룹키로 암호화한 사용자 고유 아이디)
        @NotBlank String encUserId, //그룹키로 암호화한 사용자 고유 아이디
        @NotBlank String encGroupKey, //개인키로 암호화한 그룹키
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String lookupId,
        @NotNull Integer lookupVersion
) {
    public CreateGroup2Request(
            String groupId,
            String encGroupId,
            String encencGroupMemberId,
            String encUserId,
            String encGroupKey
    ) {
        this(groupId, encGroupId, encencGroupMemberId, encUserId, encGroupKey, "0".repeat(64), 1);
    }
}
