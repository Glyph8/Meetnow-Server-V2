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
    private static final String UNKNOWN_ENDPOINT = "unknown";

    private GroupLookupSupport() {
    }

    public static boolean hasLookup(String lookupId, Integer lookupVersion) {
        return lookupId != null && !lookupId.isBlank() && lookupVersion != null;
    }

    public static Lookup resolveLookupForWrite(String lookupId, Integer lookupVersion, String groupId) {
        return resolveLookupForWrite(lookupId, lookupVersion, groupId, UNKNOWN_ENDPOINT);
    }

    public static Lookup resolveLookupForWrite(String lookupId, Integer lookupVersion, String groupId, String endpoint) {
        if (groupId == null || groupId.isBlank()) {
            validationFailCounter("missing_group_id", endpoint).increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_INVALID_FORMAT,
                    "[ERROR]: groupId is required for group lookup write"
            );
        }
        boolean hasLookupId = lookupId != null && !lookupId.isBlank();
        boolean hasLookupVersion = lookupVersion != null;
        if (hasLookupId != hasLookupVersion) {
            validationFailCounter("lookup_pair_mismatch", endpoint).increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_INVALID_FORMAT,
                    "[ERROR]: lookupId and lookupVersion must be provided together"
            );
        }
        if (!hasLookup(lookupId, lookupVersion)) {
            validationFailCounter("missing_lookup", endpoint).increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_LEGACY_FALLBACK_DISABLED,
                    "[ERROR]: lookupId and lookupVersion are required for group lookup write"
            );
        }
        validateLookup(lookupId, lookupVersion, endpoint);
        successCounter("lookup", endpoint).increment();
        return new Lookup(lookupId, lookupVersion);
    }

    /**
     * @deprecated userId is no longer used for lookup resolution. Use
     * {@link #resolveLookupForWrite(String, Integer, String)} instead.
     */
    @Deprecated(forRemoval = false, since = "2026-04")
    public static Lookup resolveLookupForWrite(String lookupId, Integer lookupVersion, String userId, String groupId) {
        return resolveLookupForWrite(lookupId, lookupVersion, groupId, UNKNOWN_ENDPOINT);
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
        return findGroupProxyUserWithFallback(
                repository,
                userId,
                groupId,
                lookupId,
                lookupVersion,
                encGroupId,
                fallbackEnabled,
                UNKNOWN_ENDPOINT
        );
    }

    public static Optional<GroupProxyUser> findGroupProxyUserWithFallback(
            GroupProxyUserRepository repository,
            String userId,
            String groupId,
            String lookupId,
            Integer lookupVersion,
            String encGroupId,
            boolean fallbackEnabled,
            String endpoint
    ) {
        boolean hasLookup = hasLookup(lookupId, lookupVersion);
        if (hasLookup) {
            if (groupId == null || groupId.isBlank()) {
                validationFailCounter("missing_group_id", endpoint).increment();
                throw new GroupLookupValidationException(
                        BaseErrorCode.LOOKUP_INVALID_FORMAT,
                        "[ERROR]: groupId is required for lookup-based group proxy query"
                );
            }
            validateLookup(lookupId, lookupVersion, endpoint);
            Optional<GroupProxyUser> byLookup = repository.findByUserIdAndGroupIdAndLookup(userId, groupId, lookupId, lookupVersion);
            if (byLookup.isPresent()) {
                successCounter("lookup", endpoint).increment();
                return byLookup;
            }
            if (fallbackEnabled) {
                Optional<GroupProxyUser> byGroupId = repository.findByUserIdAndGroupId(userId, groupId);
                if (byGroupId.isPresent()) {
                    fallbackCounter("group_id", endpoint).increment();
                    successCounter("group_id", endpoint).increment();
                    log.info("group lookup fallback hit by groupId: endpoint={}, userId={}, groupId={}, lookupId={}", endpoint, userId, groupId, maskLookupId(lookupId));
                    return byGroupId;
                }
            }
            notFoundCounter("lookup", endpoint).increment();
            if (encGroupId == null || encGroupId.isBlank()) {
                return Optional.empty();
            }
        }

        if (!fallbackEnabled) {
            validationFailCounter("fallback_disabled", endpoint).increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_LEGACY_FALLBACK_DISABLED,
                    "[ERROR]: legacy encGroupId fallback is disabled"
            );
        }
        if (encGroupId == null || encGroupId.isBlank()) {
            validationFailCounter("missing_lookup_or_enc_group_id", endpoint).increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_INVALID_FORMAT,
                    "[ERROR]: either (lookupId, lookupVersion) or encGroupId is required"
            );
        }
        Optional<GroupProxyUser> byEncGroupId = repository.findByUserIdAndEncGroupId(userId, encGroupId);
        byEncGroupId.ifPresent(result -> {
            fallbackCounter("enc_group_id", endpoint).increment();
            successCounter("enc_group_id", endpoint).increment();
            log.info("group lookup fallback hit by encGroupId: endpoint={}, userId={}, lookupId={}", endpoint, userId, maskLookupId(lookupId));
        });
        if (byEncGroupId.isEmpty()) {
            notFoundCounter("enc_group_id", endpoint).increment();
        }
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
        return findGroupProxyUserWithFallbackOrThrow(
                repository,
                userId,
                groupId,
                lookupId,
                lookupVersion,
                encGroupId,
                fallbackEnabled,
                notFoundExceptionSupplier,
                UNKNOWN_ENDPOINT
        );
    }

    public static GroupProxyUser findGroupProxyUserWithFallbackOrThrow(
            GroupProxyUserRepository repository,
            String userId,
            String groupId,
            String lookupId,
            Integer lookupVersion,
            String encGroupId,
            boolean fallbackEnabled,
            Supplier<? extends RuntimeException> notFoundExceptionSupplier,
            String endpoint
    ) {
        return findGroupProxyUserWithFallback(
                repository,
                userId,
                groupId,
                lookupId,
                lookupVersion,
                encGroupId,
                fallbackEnabled,
                endpoint
        ).orElseThrow(notFoundExceptionSupplier);
    }

    public static void validateLookup(String lookupId, Integer lookupVersion) {
        validateLookup(lookupId, lookupVersion, UNKNOWN_ENDPOINT);
    }

    public static void validateLookup(String lookupId, Integer lookupVersion, String endpoint) {
        if (lookupVersion == null || lookupVersion != LOOKUP_VERSION_V1) {
            validationFailCounter("invalid_lookup_version", endpoint).increment();
            throw new GroupLookupValidationException(
                    BaseErrorCode.LOOKUP_VERSION_UNSUPPORTED,
                    "[ERROR]: invalid group lookupVersion=" + lookupVersion
            );
        }
        versionCounter(lookupVersion, endpoint).increment();
        if (lookupId == null || !LOOKUP_ID_PATTERN.matcher(lookupId).matches()) {
            validationFailCounter("invalid_lookup_id_format", endpoint).increment();
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

    private static io.micrometer.core.instrument.Counter successCounter(String path, String endpoint) {
        return Metrics.counter("group.lookup.success.count", "path", path, "endpoint", endpointTag(endpoint));
    }

    private static io.micrometer.core.instrument.Counter fallbackCounter(String path, String endpoint) {
        return Metrics.counter("group.lookup.fallback.count", "path", path, "endpoint", endpointTag(endpoint));
    }

    private static io.micrometer.core.instrument.Counter validationFailCounter(String reason, String endpoint) {
        return Metrics.counter("group.lookup.validation.fail.count", "reason", reason, "endpoint", endpointTag(endpoint));
    }

    private static io.micrometer.core.instrument.Counter notFoundCounter(String path, String endpoint) {
        return Metrics.counter("group.lookup.not_found.count", "path", path, "endpoint", endpointTag(endpoint));
    }

    private static io.micrometer.core.instrument.Counter versionCounter(Integer lookupVersion, String endpoint) {
        return Metrics.counter("group.lookup.version.count", "lookupVersion", String.valueOf(lookupVersion), "endpoint", endpointTag(endpoint));
    }

    private static String endpointTag(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return UNKNOWN_ENDPOINT;
        }
        return endpoint;
    }
}
