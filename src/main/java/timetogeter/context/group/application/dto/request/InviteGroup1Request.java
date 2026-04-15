package timetogeter.context.group.application.dto.request;

public record InviteGroup1Request(
        String groupId,
        String encGroupId,
        String lookupId,
        Integer lookupVersion
) {
}
