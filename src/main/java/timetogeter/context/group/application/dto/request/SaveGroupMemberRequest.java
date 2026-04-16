package timetogeter.context.group.application.dto.request;

public record SaveGroupMemberRequest(
        String groupId,           // 그룹 아이디
        String encGroupKey,       // 개인키로 암호화한 그룹키
        String encUserId,         // 그룹키로 암호화한 사용자 고유 아이디
        String encGroupId,        // 개인키로 암호화한 그룹 아이디
        String encencGroupMemberId, // 개인키로 암호화한 encUserId
        String lookupId,
        Integer lookupVersion
) {
}
