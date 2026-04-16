package timetogeter.context.group.application.dto.request;

public record LeaveGroup2Request(
        boolean isManager,
        String groupId,
        String encGroupId,
        String lookupId,
        Integer lookupVersion

) {
}
