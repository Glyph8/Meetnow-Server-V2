package timetogeter.context.group.application.dto.request;

public record LeavGroup1Request(
        String groupId,
        String encGroupId, //개인키로 암호화한 그룹 아이디(하위 호환 fallback용)
        String lookupId,
        Integer lookupVersion
) {
}
