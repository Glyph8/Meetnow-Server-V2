package timetogeter.context.promise.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timetogeter.context.promise.domain.entity.PromiseShareKey;
import timetogeter.context.promise.domain.repository.custom.PromiseShareRepositoryCustom;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromiseShareKeyRepository extends JpaRepository<PromiseShareKey, String>, PromiseShareRepositoryCustom {
    @Query(value = "SELECT schedule_id FROM promise_share_key WHERE enc_promise_key IN (:encPromiseKeyList)", nativeQuery = true)
    List<String> findScheduleIdsByEncPromiseKeyList(@Param("encPromiseKeyList") List<String> encPromiseKeyList);

    List<PromiseShareKey> findByPromiseId(@Param("promiseId")String promiseId);

    @Query(value = "SELECT * FROM promise_share_key WHERE enc_promise_key IN (:encPromiseKeyList)", nativeQuery = true)
    List<PromiseShareKey> findByEncPromiseKeyIn(@Param("encPromiseKeyList") List<String> encPromiseKeyList);

    @Query(value = "SELECT * FROM promise_share_key WHERE enc_promise_key = :encPromiseKey AND enc_user_id = :encUserId", nativeQuery = true)
    Optional<PromiseShareKey> findByEncPromiseKeyAndEncUserId(
            @Param("encPromiseKey") String encPromiseKey,
            @Param("encUserId") String encUserId
    );

    @Query(value = "SELECT enc_promise_key FROM promise_share_key WHERE promise_id IN (:promiseIdList)", nativeQuery = true)
    List<String> findEncPromiseKeysByPromiseIdIn(@Param("promiseIdList") List<String> promiseIdList);

    @Query(value = "SELECT DISTINCT schedule_id FROM promise_share_key WHERE promise_id IN (:promiseIdList) AND schedule_id IS NOT NULL", nativeQuery = true)
    List<String> findScheduleIdsByPromiseIdIn(@Param("promiseIdList") List<String> promiseIdList);

    @Query(value = "SELECT * FROM promise_share_key WHERE schedule_id IN (:scheduleIdList) AND schedule_id IS NOT NULL", nativeQuery = true)
    List<PromiseShareKey> findByScheduleIdIn(@Param("scheduleIdList") List<String> scheduleIdList);


    @Query("SELECT p.encUserId FROM PromiseShareKey p WHERE p.promiseId = :promiseId")
    List<String> findUserIdsByPromiseId(@Param("promiseId")String promiseId);

    @Query("SELECT p FROM PromiseShareKey p WHERE p.encPromiseKey = :encPromiseKey")
    Optional<PromiseShareKey> findByEncPromiseKey(@Param("encPromiseKey")String encPromiseKey);

    @Query(value = "SELECT enc_promise_key " +
            "FROM promise_share_key " +
            "WHERE promise_id = :promiseId AND enc_user_id = :encUserId",
            nativeQuery = true)
    Optional<String> findEncPromiseKey(@Param("promiseId") String promiseId,
                                       @Param("encUserId") String encUserId);

    @Query(value = "SELECT enc_promise_key " +
            "FROM promise_share_key " +
            "WHERE promise_id = :promiseId AND lookup_id = :lookupId AND lookup_version = :lookupVersion",
            nativeQuery = true)
    Optional<String> findEncPromiseKeyByLookup(@Param("promiseId") String promiseId,
                                               @Param("lookupId") String lookupId,
                                               @Param("lookupVersion") Integer lookupVersion);

    @Query(value = "SELECT * FROM promise_share_key " +
            "WHERE promise_id = :promiseId AND lookup_id = :lookupId AND lookup_version = :lookupVersion",
            nativeQuery = true)
    Optional<PromiseShareKey> findByPromiseIdAndLookup(@Param("promiseId") String promiseId,
                                                       @Param("lookupId") String lookupId,
                                                       @Param("lookupVersion") Integer lookupVersion);

    Optional<PromiseShareKey> findByPromiseIdAndEncUserId(String promiseId, String encUserId);

    boolean existsByPromiseIdAndUserId(String promiseId, String userId);

    boolean existsByPromiseIdAndEncUserId(String promiseId, String encUserId);

    @Modifying
    @Query("DELETE FROM PromiseShareKey p WHERE p.promiseId = :promiseId")
    void deleteAllByEncPromiseId(@Param("promiseId") String promiseId);
}
