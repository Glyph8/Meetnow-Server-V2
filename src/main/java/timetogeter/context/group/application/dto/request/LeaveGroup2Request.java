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
public record LeaveGroup2Request(
        boolean isManager,
        @NotBlank String groupId,
        String encGroupId,
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String lookupId,
        @NotNull Integer lookupVersion

) {
}
