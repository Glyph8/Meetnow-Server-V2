package timetogeter.context.group.application.support;

import timetogeter.context.group.domain.entity.GroupProxyUser;
import timetogeter.context.group.domain.repository.GroupProxyUserRepository;
import timetogeter.context.group.exception.GroupLookupValidationException;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class GroupLookupSupport {
    public static final int LOOKUP_VERSION_V1 = 1;
    private static final Pattern LOOKUP_ID_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private GroupLookupSupport() {
    }

    public static boolean hasLookup(String lookupId, Integer lookupVersion) {
        return lookupId != null && !lookupId.isBlank() && lookupVersion != null;
    }

    public static Lookup resolveLookupForWrite(String lookupId, Integer lookupVersion, String userId, String groupId) {
        if (!hasLookup(lookupId, lookupVersion)) {
            return new Lookup(generateLookupId(userId, groupId), LOOKUP_VERSION_V1);
        }
        validateLookup(lookupId, lookupVersion);
        return new Lookup(lookupId, lookupVersion);
    }

    public static Optional<GroupProxyUser> findGroupProxyUserWithFallback(
            GroupProxyUserRepository repository,
            String userId,
            String groupId,
            String lookupId,
            Integer lookupVersion,
            String encGroupId
    ) {
        boolean hasLookup = hasLookup(lookupId, lookupVersion);
        if (hasLookup) {
            if (groupId == null || groupId.isBlank()) {
                throw new GroupLookupValidationException(
                        BaseErrorCode.BAD_REQUEST,
                        "[ERROR]: groupId is required for lookup-based group proxy query"
                );
            }
            validateLookup(lookupId, lookupVersion);
            Optional<GroupProxyUser> byLookup = repository.findByUserIdAndGroupIdAndLookup(userId, groupId, lookupId, lookupVersion);
            if (byLookup.isPresent()) {
                return byLookup;
            }
            Optional<GroupProxyUser> byGroupId = repository.findByUserIdAndGroupId(userId, groupId);
            if (byGroupId.isPresent()) {
                return byGroupId;
            }
            if (encGroupId == null || encGroupId.isBlank()) {
                return Optional.empty();
            }
        }

        if (encGroupId == null || encGroupId.isBlank()) {
            throw new GroupLookupValidationException(
                    BaseErrorCode.BAD_REQUEST,
                    "[ERROR]: either (lookupId, lookupVersion) or encGroupId is required"
            );
        }
        return repository.findByUserIdAndEncGroupId(userId, encGroupId);
    }

    public static GroupProxyUser findGroupProxyUserWithFallbackOrThrow(
            GroupProxyUserRepository repository,
            String userId,
            String groupId,
            String lookupId,
            Integer lookupVersion,
            String encGroupId,
            Supplier<? extends RuntimeException> notFoundExceptionSupplier
    ) {
        return findGroupProxyUserWithFallback(
                repository,
                userId,
                groupId,
                lookupId,
                lookupVersion,
                encGroupId
        ).orElseThrow(notFoundExceptionSupplier);
    }

    public static void validateLookup(String lookupId, Integer lookupVersion) {
        if (lookupVersion == null || lookupVersion != LOOKUP_VERSION_V1) {
            throw new GroupLookupValidationException(
                    BaseErrorCode.BAD_REQUEST,
                    "[ERROR]: invalid group lookupVersion=" + lookupVersion
            );
        }
        if (lookupId == null || !LOOKUP_ID_PATTERN.matcher(lookupId).matches()) {
            throw new GroupLookupValidationException(
                    BaseErrorCode.BAD_REQUEST,
                    "[ERROR]: invalid group lookupId format"
            );
        }
    }

    public static String generateLookupId(String userId, String groupId) {
        try {
            String safeUserId = userId == null ? "" : userId;
            String safeGroupId = groupId == null ? "" : groupId;
            String key = safeUserId.length() + ":" + safeUserId + "|" + safeGroupId.length() + ":" + safeGroupId;
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new GroupLookupValidationException(
                    BaseErrorCode.INTERNAL_SERVER_ERROR,
                    "[ERROR]: lookupId generation failed"
            );
        }
    }

    public record Lookup(String lookupId, Integer lookupVersion) {
    }
}
