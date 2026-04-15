package timetogeter.context.group.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupProxyUser {
    @Id
    private String groupProxyId;

    private String userId;
    private String groupId;
    private String encGroupId;
    private String lookupId;
    private Integer lookupVersion;

    @CreationTimestamp
    private LocalDateTime timestamp;
    private String encGroupMemberId;

    private GroupProxyUser(
            String userId,
            String groupId,
            String encGroupId,
            String lookupId,
            Integer lookupVersion,
            LocalDateTime timestamp,
            String encGroupMemberId
    ) {
        this.groupProxyId = UUID.randomUUID().toString();
        this.userId = userId;
        this.groupId = groupId;
        this.encGroupId = encGroupId;
        this.lookupId = lookupId;
        this.lookupVersion = lookupVersion;
        this.timestamp = timestamp;
        this.encGroupMemberId = encGroupMemberId;
    }

    public static GroupProxyUser of(
            String userId,
            String groupId,
            String encGroupId,
            String lookupId,
            Integer lookupVersion,
            String encGroupMemberId,
            long timestampMillis
    ) {
        LocalDateTime timestamp = new Timestamp(timestampMillis).toLocalDateTime();
        return new GroupProxyUser(userId, groupId, encGroupId, lookupId, lookupVersion, timestamp, encGroupMemberId);
    }
}
