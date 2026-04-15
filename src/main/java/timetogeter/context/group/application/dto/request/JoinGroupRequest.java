package timetogeter.context.group.application.dto.request;

public record JoinGroupRequest(
        String encryptedValue,
        String groupId,
        String encGroupId,
        String encGroupKey,
        String encUserId,
        String encencGroupMemberId,
        String lookupId,
        Integer lookupVersion
) {
}
