package timetogeter.context.promise.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import timetogeter.context.auth.domain.repository.UserRepository;
import timetogeter.context.group.domain.repository.GroupProxyUserRepository;
import timetogeter.context.group.domain.repository.GroupRepository;
import timetogeter.context.group.domain.repository.GroupShareKeyRepository;
import timetogeter.context.promise.application.dto.request.manage.GetPromiseRequest;
import timetogeter.context.promise.application.dto.request.manage.JoinPromise1Request;
import timetogeter.context.promise.application.dto.response.manage.GetPromiseKey2;
import timetogeter.context.promise.application.dto.response.manage.JoinPromise1Response;
import timetogeter.context.promise.domain.entity.Promise;
import timetogeter.context.promise.domain.entity.PromiseShareKey;
import timetogeter.context.promise.domain.repository.PromiseProxyUserRepository;
import timetogeter.context.promise.domain.repository.PromiseRepository;
import timetogeter.context.promise.domain.repository.PromiseShareKeyRepository;
import timetogeter.context.promise.exception.PromiseLookupForbiddenException;
import timetogeter.context.promise.exception.PromiseLookupValidationException;
import timetogeter.context.promise.exception.PromiseMemberKeyConflictException;
import timetogeter.context.promise.exception.PromiseNotFoundException;
import timetogeter.global.mail.EmailService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromiseManageInfoServiceTest {

    @Mock
    private GroupProxyUserRepository groupProxyUserRepository;
    @Mock
    private GroupShareKeyRepository groupShareKeyRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private PromiseRepository promiseRepository;
    @Mock
    private PromiseProxyUserRepository promiseProxyUserRepository;
    @Mock
    private PromiseShareKeyRepository promiseShareKeyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private EmailService emailService;

    private SimpleMeterRegistry meterRegistry;
    private PromiseManageInfoService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new PromiseManageInfoService(
                groupProxyUserRepository,
                groupShareKeyRepository,
                groupRepository,
                promiseRepository,
                promiseProxyUserRepository,
                promiseShareKeyRepository,
                userRepository,
                redisTemplate,
                emailService,
                meterRegistry
        );
    }

    @Test
    @DisplayName("lookupVersion 이 1이 아니면 400 예외를 반환한다")
    void getPromiseKey2_invalidLookupVersion() {
        GetPromiseRequest request = new GetPromiseRequest("promise-1", "a".repeat(64), 2, "enc-user");
        assertThrows(PromiseLookupValidationException.class, () -> service.getPromiseKey2("user-1", request));
    }

    @Test
    @DisplayName("promisekey2 는 lookup 조회를 우선 사용한다")
    void getPromiseKey2_lookupFirst() {
        Promise promise = mock(Promise.class);
        when(promise.getManagerId()).thenReturn("manager-1");
        when(promiseRepository.findById("promise-1")).thenReturn(Optional.of(promise));
        when(promiseShareKeyRepository.existsByPromiseIdAndUserId("promise-1", "user-1")).thenReturn(true);
        when(promiseShareKeyRepository.findEncPromiseKeyByLookup("promise-1", "a".repeat(64), 1)).thenReturn(Optional.of("lookup-key"));

        GetPromiseKey2 response = service.getPromiseKey2("user-1", new GetPromiseRequest("promise-1", "a".repeat(64), 1, "enc-user"));

        assertThat(response.encPromiseKey()).isEqualTo("lookup-key");
        verify(promiseShareKeyRepository, never()).findEncPromiseKey("promise-1", "enc-user");
    }

    @Test
    @DisplayName("lookup 조회 실패 시 fallback 조회를 사용한다")
    void getPromiseKey2_fallbackWhenLookupMiss() {
        Promise promise = mock(Promise.class);
        when(promise.getManagerId()).thenReturn("manager-1");
        when(promiseRepository.findById("promise-1")).thenReturn(Optional.of(promise));
        when(promiseShareKeyRepository.existsByPromiseIdAndUserId("promise-1", "user-1")).thenReturn(true);
        when(promiseShareKeyRepository.findEncPromiseKeyByLookup("promise-1", "a".repeat(64), 1)).thenReturn(Optional.empty());
        when(promiseShareKeyRepository.findEncPromiseKey("promise-1", "enc-user")).thenReturn(Optional.of("fallback-key"));

        GetPromiseKey2 response = service.getPromiseKey2("user-1", new GetPromiseRequest("promise-1", "a".repeat(64), 1, "enc-user"));

        assertThat(response.encPromiseKey()).isEqualTo("fallback-key");
        verify(promiseShareKeyRepository).findEncPromiseKey("promise-1", "enc-user");
    }

    @Test
    @DisplayName("권한 없는 사용자가 promisekey2 조회하면 403 예외를 반환한다")
    void getPromiseKey2_forbidden() {
        Promise promise = mock(Promise.class);
        when(promise.getManagerId()).thenReturn("manager-1");
        when(promiseRepository.findById("promise-1")).thenReturn(Optional.of(promise));
        when(promiseShareKeyRepository.existsByPromiseIdAndUserId("promise-1", "user-1")).thenReturn(false);
        when(promiseShareKeyRepository.existsByPromiseIdAndEncUserId("promise-1", "enc-user")).thenReturn(false);

        GetPromiseRequest request = new GetPromiseRequest("promise-1", "a".repeat(64), 1, "enc-user");
        assertThrows(PromiseLookupForbiddenException.class, () -> service.getPromiseKey2("user-1", request));
    }

    @Test
    @DisplayName("조회 결과가 없으면 404 예외를 반환한다")
    void getPromiseKey2_notFound() {
        Promise promise = mock(Promise.class);
        when(promise.getManagerId()).thenReturn("manager-1");
        when(promiseRepository.findById("promise-1")).thenReturn(Optional.of(promise));
        when(promiseShareKeyRepository.existsByPromiseIdAndUserId("promise-1", "user-1")).thenReturn(true);
        when(promiseShareKeyRepository.findEncPromiseKeyByLookup("promise-1", "a".repeat(64), 1)).thenReturn(Optional.empty());
        when(promiseShareKeyRepository.findEncPromiseKey("promise-1", "enc-user")).thenReturn(Optional.empty());

        GetPromiseRequest request = new GetPromiseRequest("promise-1", "a".repeat(64), 1, "enc-user");
        assertThrows(PromiseNotFoundException.class, () -> service.getPromiseKey2("user-1", request));
    }

    @Test
    @DisplayName("joinPromise1 중복 요청(동일 lookup, 동일 key)은 idempotent 성공한다")
    void joinPromise1_idempotentSuccess() {
        Promise promise = mock(Promise.class);
        PromiseShareKey existing = mock(PromiseShareKey.class);
        when(promise.getTitle()).thenReturn("test-promise");
        when(promiseRepository.findById("promise-1")).thenReturn(Optional.of(promise));
        when(promiseShareKeyRepository.findByPromiseIdAndLookup("promise-1", "a".repeat(64), 1)).thenReturn(Optional.of(existing));
        when(existing.getEncPromiseKey()).thenReturn("enc-key");

        JoinPromise1Response response = service.joinPromise1("user-1",
                new JoinPromise1Request("promise-1", "enc-promise-id", "enc-promise-member-id", "enc-user", "enc-key", "a".repeat(64), 1));

        assertThat(response.message()).contains("약속에 참여하였습니다.");
        verify(promiseShareKeyRepository, never()).save(any(PromiseShareKey.class));
        verify(promiseRepository, never()).save(any(Promise.class));
    }

    @Test
    @DisplayName("joinPromise1 중복 요청에서 기존 key와 다르면 409 예외를 반환한다")
    void joinPromise1_conflictWhenKeyMismatch() {
        Promise promise = mock(Promise.class);
        PromiseShareKey existing = mock(PromiseShareKey.class);
        when(promiseRepository.findById("promise-1")).thenReturn(Optional.of(promise));
        when(promiseShareKeyRepository.findByPromiseIdAndLookup("promise-1", "a".repeat(64), 1)).thenReturn(Optional.of(existing));
        when(existing.getEncPromiseKey()).thenReturn("old-key");

        JoinPromise1Request request = new JoinPromise1Request(
                "promise-1", "enc-promise-id", "enc-promise-member-id", "enc-user", "new-key", "a".repeat(64), 1
        );
        assertThrows(PromiseMemberKeyConflictException.class, () -> service.joinPromise1("user-1", request));
        assertThat(meterRegistry.counter("join.lookup.unique.conflict.count").count()).isEqualTo(1.0d);
    }
}
