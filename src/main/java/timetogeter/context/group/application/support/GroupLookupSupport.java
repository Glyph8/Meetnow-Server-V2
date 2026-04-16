package timetogeter.context.group.application.support;

import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public final class GroupLookupSupport {
    public static final int LOOKUP_VERSION_V1 = 1;
    private static final Pattern LOOKUP_ID_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private GroupLookupSupport() {
    }

    public static boolean hasLookup(String lookupId, Integer lookupVersion) {
        return lookupId != null && !lookupId.isBlank() && lookupVersion != null;
    }

    public static Lookup resolveLookupForWrite(String lookupId, Integer lookupVersion, String groupId) {
        if (groupId == null || groupId.isBlank()) {
            Metrics.counter("group.lookup.validation.fail.count", "reason", "missing_group_id").increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_INVALID_FORMAT,
                    "[ERROR]: groupId is required for group lookup write"
            );
        }
        boolean hasLookupId = lookupId != null && !lookupId.isBlank();
        boolean hasLookupVersion = lookupVersion != null;
        if (hasLookupId != hasLookupVersion) {
            Metrics.counter("group.lookup.validation.fail.count", "reason", "lookup_pair_mismatch").increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_INVALID_FORMAT,
                    "[ERROR]: lookupId and lookupVersion must be provided together"
            );
        }
        if (!hasLookup(lookupId, lookupVersion)) {
            Metrics.counter("group.lookup.validation.fail.count", "reason", "missing_lookup").increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_LEGACY_FALLBACK_DISABLED,
                    "[ERROR]: lookupId and lookupVersion are required for group lookup write"
            );
        }
        validateLookup(lookupId, lookupVersion);
        Metrics.counter("group.lookup.success.count", "path", "lookup").increment();
        return new Lookup(lookupId, lookupVersion);
    }

    public static Optional<GroupProxyUser> findGroupProxyUserWithFallback(
            GroupProxyUserRepository repository,
            String userId,
            String groupId,
            String lookupId,
            Integer lookupVersion,
            String encGroupId,
            boolean fallbackEnabled
    ) {
        boolean hasLookup = hasLookup(lookupId, lookupVersion);
        if (hasLookup) {
            if (groupId == null || groupId.isBlank()) {
                Metrics.counter("group.lookup.validation.fail.count", "reason", "missing_group_id").increment();
                throw new GroupLookupValidationException(
                        BaseErrorCode.LOOKUP_INVALID_FORMAT,
                        "[ERROR]: groupId is required for lookup-based group proxy query"
                );
            }
            validateLookup(lookupId, lookupVersion);
            Optional<GroupProxyUser> byLookup = repository.findByUserIdAndGroupIdAndLookup(userId, groupId, lookupId, lookupVersion);
            if (byLookup.isPresent()) {
                Metrics.counter("group.lookup.success.count", "path", "lookup").increment();
                return byLookup;
            }
            Optional<GroupProxyUser> byGroupId = repository.findByUserIdAndGroupId(userId, groupId);
            if (byGroupId.isPresent()) {
                Metrics.counter("group.lookup.fallback.count", "path", "group_id").increment();
                Metrics.counter("group.lookup.success.count", "path", "group_id").increment();
                log.info("group lookup fallback hit by groupId: userId={}, groupId={}, lookupId={}", userId, groupId, maskLookupId(lookupId));
                return byGroupId;
            }
            if (!fallbackEnabled) {
                return Optional.empty();
            }
            if (encGroupId == null || encGroupId.isBlank()) {
                return Optional.empty();
            }
        }

        if (!fallbackEnabled) {
            Metrics.counter("group.lookup.validation.fail.count", "reason", "fallback_disabled").increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_LEGACY_FALLBACK_DISABLED,
                    "[ERROR]: legacy encGroupId fallback is disabled"
            );
        }
        if (encGroupId == null || encGroupId.isBlank()) {
            Metrics.counter("group.lookup.validation.fail.count", "reason", "missing_lookup_or_enc_group_id").increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_INVALID_FORMAT,
                    "[ERROR]: either (lookupId, lookupVersion) or encGroupId is required"
            );
        }
        Optional<GroupProxyUser> byEncGroupId = repository.findByUserIdAndEncGroupId(userId, encGroupId);
        byEncGroupId.ifPresent(result -> {
            Metrics.counter("group.lookup.fallback.count", "path", "enc_group_id").increment();
            Metrics.counter("group.lookup.success.count", "path", "enc_group_id").increment();
            log.info("group lookup fallback hit by encGroupId: userId={}, lookupId={}", userId, maskLookupId(lookupId));
        });
        return byEncGroupId;
    }

    public static GroupProxyUser findGroupProxyUserWithFallbackOrThrow(
            GroupProxyUserRepository repository,
            String userId,
            String groupId,
            String lookupId,
            Integer lookupVersion,
            String encGroupId,
            boolean fallbackEnabled,
            Supplier<? extends RuntimeException> notFoundExceptionSupplier
    ) {
        return findGroupProxyUserWithFallback(
                repository,
                userId,
                groupId,
                lookupId,
                lookupVersion,
                encGroupId,
                fallbackEnabled
        ).orElseThrow(notFoundExceptionSupplier);
    }

    public static void validateLookup(String lookupId, Integer lookupVersion) {
        if (lookupVersion == null || lookupVersion != LOOKUP_VERSION_V1) {
            Metrics.counter("group.lookup.validation.fail.count", "reason", "invalid_lookup_version").increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_VERSION_UNSUPPORTED,
                    "[ERROR]: invalid group lookupVersion=" + lookupVersion
            );
        }
        Metrics.counter("group.lookup.version.count", "lookupVersion", String.valueOf(lookupVersion)).increment();
        if (lookupId == null || !LOOKUP_ID_PATTERN.matcher(lookupId).matches()) {
            Metrics.counter("group.lookup.validation.fail.count", "reason", "invalid_lookup_id_format").increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_INVALID_FORMAT,
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

    private static String maskLookupId(String lookupId) {
        if (lookupId == null || lookupId.length() < 8) {
            return "********";
        }
        return lookupId.substring(0, 8) + "****";
    }
}
