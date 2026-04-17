package timetogeter.context.group.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timetogeter.context.group.domain.entity.GroupProxyUser;
import java.util.*;


@Repository
public interface GroupProxyUserRepository extends JpaRepository<GroupProxyUser, String> {
    @Query("select g.encGroupId from GroupProxyUser g where g.userId = :userId")
    List<String> findEncGroupIdsByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(g) > 0 FROM GroupProxyUser g WHERE g.encGroupId = :encGroupId")
    boolean existsByEncGroupId(@Param("encGroupId") String encGroupIdCopy);

    @Query(value = "SELECT * FROM group_proxy_user WHERE user_id = :userId", nativeQuery = true)
    List<GroupProxyUser> findAllByUserId(@Param("userId") String userId);

    @Query(value = "SELECT * FROM group_proxy_user WHERE user_id = :userId AND enc_group_id = :encGroupId", nativeQuery = true)
    Optional<GroupProxyUser> findByUserIdAndEncGroupId(@Param("userId") String userId, @Param("encGroupId") String encGroupId);

    @Query(value = """
            SELECT * FROM group_proxy_user
            WHERE user_id = :userId
              AND group_id = :groupId
              AND lookup_id = :lookupId
              AND lookup_version = :lookupVersion
            """, nativeQuery = true)
    Optional<GroupProxyUser> findByUserIdAndGroupIdAndLookup(
            @Param("userId") String userId,
            @Param("groupId") String groupId,
            @Param("lookupId") String lookupId,
            @Param("lookupVersion") Integer lookupVersion
    );

    @Query(value = """
            SELECT COUNT(*) FROM group_proxy_user
            WHERE group_id = :groupId
              AND lookup_id = :lookupId
              AND lookup_version = :lookupVersion
            """, nativeQuery = true)
    long countByGroupIdAndLookup(
            @Param("groupId") String groupId,
            @Param("lookupId") String lookupId,
            @Param("lookupVersion") Integer lookupVersion
    );

    @Query(value = "SELECT * FROM group_proxy_user WHERE user_id = :userId AND group_id = :groupId", nativeQuery = true)
    Optional<GroupProxyUser> findByUserIdAndGroupId(@Param("userId") String userId, @Param("groupId") String groupId);

    @Query(value = "SELECT * FROM group_proxy_user WHERE group_id = :groupId AND enc_group_id = :encGroupId", nativeQuery = true)
    Optional<GroupProxyUser> findByGroupIdAndEncGroupId(@Param("groupId") String groupId, @Param("encGroupId") String encGroupId);

    @Query(value = "SELECT * FROM group_proxy_user WHERE enc_group_id = :encGroupId", nativeQuery = true)
    Optional<GroupProxyUser> findByEncGroupId(@Param("encGroupId") String encGroupId);
}
