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
        @Schema(description = "조회 인덱스 식별자(64-char hex)", example = "4f53cda18c2baa0c0354bb5f9a3ecbe5ed4f52f5f5c2f7f8052e8b6a85bf8d52", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String lookupId,
        @Schema(description = "조회 인덱스 버전(현재 1)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Integer lookupVersion

) {
}
