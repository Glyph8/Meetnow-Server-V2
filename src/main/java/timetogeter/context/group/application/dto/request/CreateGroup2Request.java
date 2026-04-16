package timetogeter.context.group.application.dto.request;

public record CreateGroup2Request(
        String groupId, //그룹 아이디
        String encGroupId, //개인키로 암호화한 그룹 아이디
        String encencGroupMemberId, //개인키로 암호화한 (그룹키로 암호화한 사용자 고유 아이디)
        String encUserId, //그룹키로 암호화한 사용자 고유 아이디
        String encGroupKey, //개인키로 암호화한 그룹키
        String lookupId,
        Integer lookupVersion
) {
    public CreateGroup2Request(
            String groupId,
            String encGroupId,
            String encencGroupMemberId,
            String encUserId,
            String encGroupKey
    ) {
        this(groupId, encGroupId, encencGroupMemberId, encUserId, encGroupKey, null, null);
    }
}
