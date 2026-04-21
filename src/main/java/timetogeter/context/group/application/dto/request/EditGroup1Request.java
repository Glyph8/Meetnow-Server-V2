package timetogeter.context.group.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EditGroup1Request(
        @Schema(description = "그룹 식별자", example = "grp_12345", required = true)
        @NotBlank(message = "groupId는 필수입니다.")
        String groupId,

        @Schema(description = "암호화된 그룹 ID(하위 호환 fallback용)", example = "BASE64-encGroupId==")
        String encGroupId,

        @Schema(description = "조회 인덱스 식별자(64-char hex)", example = "4f53cda18c2baa0c0354bb5f9a3ecbe5ed4f52f5f5c2f7f8052e8b6a85bf8d52")
        @NotBlank(message = "lookupId는 필수입니다.")
        @Pattern(regexp = "^[0-9a-f]{64}$", message = "lookupId 형식이 올바르지 않습니다.")
        String lookupId,

        @Schema(description = "조회 인덱스 버전(현재 1)", example = "1")
        @NotNull(message = "lookupVersion은 필수입니다.")
        Integer lookupVersion,

        @Schema(description = "수정할 그룹 이름", example = "새 그룹명")
        String groupName,

        @Schema(description = "수정할 그룹 이미지 URL", example = "https://example.com/image.png")
        String groupImg,

        @Schema(description = "그룹 설명", example = "스터디 그룹입니다.")
        String description
) {
}
