package timetogeter.context.promise.application.dto.request.manage;

import io.swagger.v3.oas.annotations.media.Schema;

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
        String promiseId,
        String encPromiseId,
        String encPromiseMemberId,
        String encUserId,
        String encPromiseKey,
        String lookupId,
        Integer lookupVersion
){
}
