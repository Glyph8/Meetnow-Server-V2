package timetogeter.context.promise.application.dto.request.manage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(requiredProperties = {
        "promiseId",
        "encPromiseId",
        "encPromiseMemberId",
        "encUserId",
        "encPromiseKey",
        "lookupId",
        "lookupVersion"
})
public record JoinPromise1Request(
        @NotBlank String promiseId,
        @NotBlank String encPromiseId,
        @NotBlank String encPromiseMemberId,
        @NotBlank String encUserId,
        @NotBlank String encPromiseKey,
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String lookupId,
        @NotNull Integer lookupVersion
){
}
