package timetogeter.context.promise.application.dto.request.basic;

public record CreatePromise1Request(
        String groupId,
        String encGroupId,
        String lookupId,
        Integer lookupVersion
) {
}
