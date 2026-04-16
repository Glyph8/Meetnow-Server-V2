package timetogeter.context.group.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(requiredProperties = {
        "encryptedValue",
        "groupId",
        "encGroupId",
        "encGroupKey",
        "encUserId",
        "encencGroupMemberId",
        "lookupId",
        "lookupVersion"
})
public record JoinGroupRequest(
        @NotBlank String encryptedValue,
        @NotBlank String groupId,
        @NotBlank String encGroupId,
        @NotBlank String encGroupKey,
        @NotBlank String encUserId,
        @NotBlank String encencGroupMemberId,
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String lookupId,
        @NotNull Integer lookupVersion
) {
}
