package timetogeter.context.promise.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "promise_share_key",
        indexes = {
                @Index(name = "idx_promise_lookup", columnList = "promise_id,lookup_version,lookup_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_promise_lookup", columnNames = {"promise_id", "lookup_version", "lookup_id"})
        }
)
public class PromiseShareKey {
    @Id
    private String promiseShareKeyId;

    private String promiseId;
    private String scheduleId;
    private String userId;
    private String encUserId;
    private String encPromiseKey;
    private String lookupId;
    private Integer lookupVersion;

    private PromiseShareKey(String promiseId, String scheduleId, String userId, String encUserId, String encPromiseKey, String lookupId, Integer lookupVersion) {
        this.promiseShareKeyId = UUID.randomUUID().toString();
        this.promiseId = promiseId;
        this.scheduleId = scheduleId;
        this.userId = userId;
        this.encUserId = encUserId;
        this.encPromiseKey = encPromiseKey;
        this.lookupId = lookupId;
        this.lookupVersion = lookupVersion;
    }

    public static PromiseShareKey of(String promiseId, String userId, String encUserId, String encPromiseKey, String scheduleId, String lookupId, Integer lookupVersion){
        return new PromiseShareKey(promiseId, scheduleId, userId, encUserId, encPromiseKey, lookupId, lookupVersion);
    }

    public void updateScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void updateLookupInfo(String lookupId, Integer lookupVersion) {
        this.lookupId = lookupId;
        this.lookupVersion = lookupVersion;
    }
}
