package timetogeter.context.promise.application.service;

import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timetogeter.context.auth.domain.repository.UserRepository;
import timetogeter.context.auth.exception.UserNotFoundException;
import timetogeter.context.group.exception.GroupIdNotFoundException;
import timetogeter.context.group.exception.GroupProxyUserNotFoundException;
import timetogeter.context.group.exception.GroupShareKeyNotFoundException;
import timetogeter.context.group.domain.entity.Group;
import timetogeter.context.group.domain.entity.GroupProxyUser;
import timetogeter.context.group.domain.repository.GroupProxyUserRepository;
import timetogeter.context.group.domain.repository.GroupRepository;
import timetogeter.context.group.domain.repository.GroupShareKeyRepository;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise1Request;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise2Request;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise3Request;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise4Request;
import timetogeter.context.promise.application.dto.request.manage.*;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise1Response;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise2Response;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise3Response;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise4Response;
import timetogeter.context.promise.application.dto.response.manage.*;
import timetogeter.context.promise.domain.entity.Promise;
import timetogeter.context.promise.domain.entity.PromiseProxyUser;
import timetogeter.context.promise.domain.repository.PromiseProxyUserRepository;
import timetogeter.context.promise.domain.repository.PromiseRepository;
import timetogeter.context.promise.domain.entity.PromiseShareKey;
import timetogeter.context.promise.domain.repository.PromiseShareKeyRepository;
import timetogeter.context.promise.exception.PromiseLookupForbiddenException;
import timetogeter.context.promise.exception.PromiseLookupValidationException;
import timetogeter.context.promise.exception.PromiseNotFoundException;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;
import timetogeter.global.mail.EmailService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
@Slf4j
public class PromiseManageInfoService {
    // lookupId 는 클라이언트가 전달하는 64자리 소문자 hex 인덱스 키만 허용한다.
    private static final Pattern LOOKUP_ID_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private static final int LOOKUP_VERSION_V1 = 1;

    private final GroupProxyUserRepository groupProxyUserRepository;
    private final GroupShareKeyRepository groupShareKeyRepository;
    private final GroupRepository groupRepository;

    private final PromiseRepository promiseRepository;
    private final PromiseProxyUserRepository promiseProxyUserRepository;
    private final PromiseShareKeyRepository promiseShareKeyRepository;

    private final UserRepository userRepository;

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final Counter promiseKeyLookupSuccessCounter;
    private final Counter promiseKeyLookupFallbackCounter;
    private final Counter joinLookupUniqueConflictCounter;
    private final Counter promiseKeyNotFoundCounter;

    @Value("${promise.promisekey2.fallback-enabled:true}")
    private boolean promiseKeyFallbackEnabled;

    public PromiseManageInfoService(
            GroupProxyUserRepository groupProxyUserRepository,
            GroupShareKeyRepository groupShareKeyRepository,
            GroupRepository groupRepository,
            PromiseRepository promiseRepository,
            PromiseProxyUserRepository promiseProxyUserRepository,
            PromiseShareKeyRepository promiseShareKeyRepository,
            UserRepository userRepository,
            StringRedisTemplate redisTemplate,
            EmailService emailService,
            MeterRegistry meterRegistry
    ) {
        this.groupProxyUserRepository = groupProxyUserRepository;
        this.groupShareKeyRepository = groupShareKeyRepository;
        this.groupRepository = groupRepository;
        this.promiseRepository = promiseRepository;
        this.promiseProxyUserRepository = promiseProxyUserRepository;
        this.promiseShareKeyRepository = promiseShareKeyRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.promiseKeyLookupSuccessCounter = Counter.builder("promisekey2.lookup.success.rate").register(meterRegistry);
        this.promiseKeyLookupFallbackCounter = Counter.builder("promisekey2.lookup.fallback.rate").register(meterRegistry);
        this.joinLookupUniqueConflictCounter = Counter.builder("join.lookup.unique_conflict.count").register(meterRegistry);
        this.promiseKeyNotFoundCounter = Counter.builder("promisekey2.not_found.count").register(meterRegistry);
    }


    //약속 만들기 - 기본 정보 입력 Step1 - 메인 서비스 메소드
    @Transactional
    public CreatePromise1Response createPromise1(String userId, CreatePromise1Request request) {
        String encGroupId = request.encGroupId();
        GroupProxyUser groupProxyUserFound = groupProxyUserRepository.findByUserIdAndEncGroupId(userId,encGroupId)
                .orElseThrow(() -> new GroupProxyUserNotFoundException(BaseErrorCode.GROUP_PROXY_USER_NOT_FOUND, "[ERROR]: 해당 유저의 그룹 프록시 정보가 없습니다."));

        /*
        '개인키로 암호화된 그룹 아이디'-> encGroupId
        '개인키로 암호화한 (그룹키로 암호화한 사용자 고유 아이디)' -> encGroupMemberId
         */
        return new CreatePromise1Response(
                groupProxyUserFound.getEncGroupId(),
                groupProxyUserFound.getEncGroupMemberId()
        );
    }

    //약속 만들기 - 기본 정보 입력 Step2 - 메인 서비스 메소드
    @Transactional
    public CreatePromise2Response createPromise2(String userId, CreatePromise2Request request) {
        String groupId = request.groupId();
        String encGroupMemberId = request.encGroupMemberId();

        String encGroupKey = groupShareKeyRepository.findEncGroupKey(groupId, encGroupMemberId)
                .orElseThrow(() -> new GroupShareKeyNotFoundException(
                        BaseErrorCode.GROUP_SHARE_KEY_NOT_FOUND,
                        "[ERROR]: 그룹 키 조회에 실패했습니다. groupId=" + groupId + ", encGroupMemberId=" + encGroupMemberId
                ));

        return new CreatePromise2Response(encGroupKey);
    }

    //약속 만들기 - 기본 정보 입력 Step3 - 메인 서비스 메소드
    @Transactional
    public CreatePromise3Response createPromise3(CreatePromise3Request request) {


        String groupId = request.groupId();

        // 그룹 정보 조회
        Group group = groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: " + groupId));

        // 해당 그룹에 속한 사용자 encId 목록 조회
        List<String> encUserIdList = groupShareKeyRepository.findEncUserIdsByGroupId(groupId);

        // 응답 객체 생성
        CreatePromise3Response response = new CreatePromise3Response(
                group.getGroupId(),
                group.getGroupName(),
                group.getGroupImg(),
                group.getManagerId(),
                encUserIdList
        );


        return response;
    }

    //약속 만들기 - 기본 정보 입력 Step4 - 메인 서비스 메소드
    @Transactional
    public CreatePromise4Response createPromise4(String userId, CreatePromise4Request request) {

        //Promise에 약속(임의)저장하기
        Promise savedPromise = createPromise(request);
        return new CreatePromise4Response(savedPromise.getPromiseId());
    }

    //약속 만들기 - 기본 정보 입력 - 서브 서비스 메소드(1)
    @Transactional
    public Promise createPromise(CreatePromise4Request request) {
        Promise promise = Promise.of(
                request.groupId(),
                request.title(),
                request.type(),
                request.promiseImg(),
                request.managerId(),
                request.startDate(),
                request.endDate()
        );

        return promiseRepository.save(promise);
    }


    //약속 만들기 - 초대하기 Step1 - 메인 서비스 메소드
    @Transactional
    public InvitePromise1Response invitePromise1(InvitePromise1Request request) {
        List<Map<String, Integer>> whichUserIdIn = request.whichUserIdIn();

        List<String> emailList = new ArrayList<>();

        for (Map<String, Integer> userMap : whichUserIdIn) {
            for (String userId : userMap.keySet()) {
                // userRepository에서 userId로 이메일 조회
                String email = userRepository.findByUserId(userId)
                        .orElseThrow(() -> new UserNotFoundException(BaseErrorCode.INVALID_USER, "[ERROR]: 존재하지 않는 유저입니다."))
                        .getEmail();
                emailList.add(email);
            }
        }
        //메일들로 가입 링크 전송
        String joinLinkUrl = "https://meetnow.duckdns.org/promise/join1";
        String subject = "[MeetNow] 약속 참여 링크 안내";
        String contentTemplate = """
                안녕하세요,
                
                아래 링크를 통해 약속 참여를 완료해 주세요.
                %s
                
                만약 본 메일을 요청하지 않으셨다면, 이 메일은 무시하셔도 됩니다.
                
                감사합니다.
                MeetNow 드림
                """;
        for (String email : emailList) {
            String content = contentTemplate.formatted(joinLinkUrl);
            emailService.sendPlainText(email, subject, content);
        }

        return new InvitePromise1Response(emailList, "참여 링크를 이메일로 전송했어요.");
    }

    //약속 만들기 - 참여하기 Step1 - 메인 서비스 메소드
    @Transactional
    public JoinPromise1Response joinPromise1(String userId, JoinPromise1Request request) {
        validateLookup(request.lookupId(), request.lookupVersion());

        Promise promise = promiseRepository.findById(request.promiseId())
                .orElseThrow(() -> new PromiseNotFoundException(BaseErrorCode.PROMISE_NOT_FOUND,
                        "promiseId=" + request.promiseId() + " 약속을 찾을 수 없습니다"
                ));

        PromiseShareKey existingLookupRecord = promiseShareKeyRepository
                .findByPromiseIdAndLookup(request.promiseId(), request.lookupId(), request.lookupVersion())
                .orElse(null);

        if (existingLookupRecord != null) {
            // AES-GCM 랜덤 IV 특성상 암호문 문자열 비교로 평문 동일성을 판정할 수 없어 중복 lookup 요청은 idempotent 성공 처리한다.
            return new JoinPromise1Response(promise.getTitle() + " 약속에 참여하였습니다.");
        }

        PromiseShareKey existingEncUserRecord = promiseShareKeyRepository
                .findByPromiseIdAndEncUserId(request.promiseId(), request.encUserId())
                .orElse(null);

        if (existingEncUserRecord != null) {
            // 레거시 경로(encUserId)로 이미 참여한 경우에도 lookup 컬럼만 보강하고 idempotent 성공 처리한다.
            existingEncUserRecord.updateLookupInfo(request.lookupId(), request.lookupVersion());
            return new JoinPromise1Response(promise.getTitle() + " 약속에 참여하였습니다.");
        }

        //약속을 만든 userId 사용자를 PromiseProxyUser 테이블에 저장
        //약속을 만든 userId 사용자를 PromiseShareKey 테이블에 저장
        saveCreatorToPromiseTables(userId, request);

        //약속 참여자 수 증가
        promise.incrementNum();
        promiseRepository.save(promise);

        String promiseName = promise.getTitle(); //약속 제목

        return new JoinPromise1Response(promiseName + " 약속에 참여하였습니다.");
    }

    //약속 만들기 - 참여하기 Step1 - 서브 서비스 메소드(1)
    private void saveCreatorToPromiseTables(String userId, JoinPromise1Request request) {
        // PromiseProxyUser 저장
        promiseProxyUserRepository.save(PromiseProxyUser.of(
                userId,
                request.encPromiseId(),
                LocalDateTime.now(),
                request.encPromiseMemberId()
        ));

        // PromiseShareKey 저장
        promiseShareKeyRepository.save(PromiseShareKey.of(
                request.promiseId(),
                userId,
                request.encUserId(),
                request.encPromiseKey(),
                UUID.randomUUID().toString(),
                request.lookupId(),
                request.lookupVersion()
        ));
    }

    //promisekey를 획득하는 과정 - 메인 메소드(1)
    public GetPromiseKey1 getPromiseKey1(String userId) {
        List<String> encPromiseIdList = promiseProxyUserRepository.findPromiseIdsByUserId(userId);
        return new GetPromiseKey1(encPromiseIdList);
    }

    //promisekey를 획득하는 과정 - 메인 메소드(2)
    public GetPromiseKey2 getPromiseKey2(String userId, GetPromiseRequest request) {
        String promiseId = request.promiseId();
        String lookupId = request.lookupId();
        Integer lookupVersion = request.lookupVersion();
        String encUserId = request.encUserId();

        validateLookup(lookupId, lookupVersion);

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new PromiseNotFoundException(
                        BaseErrorCode.PROMISE_NOT_FOUND,
                        "[ERROR]: promiseId=" + promiseId + "에 해당하는 약속이 없습니다."
                ));

        boolean hasPermission = promiseShareKeyRepository.existsByPromiseIdAndUserId(promiseId, userId)
                || promise.getManagerId().equals(userId)
                || (encUserId != null && promiseShareKeyRepository.existsByPromiseIdAndEncUserId(promiseId, encUserId));

        if (!hasPermission) {
            throw new PromiseLookupForbiddenException(
                    BaseErrorCode.PROMISE_LOOKUP_FORBIDDEN,
                    "[ERROR]: unauthorized promisekey2 lookup. promiseId=" + promiseId + ", userId=" + userId + ", lookupId=" + maskLookupId(lookupId)
            );
        }


        String encPromiseKey = promiseShareKeyRepository
                .findEncPromiseKeyByLookup(promiseId, lookupId, lookupVersion)
                .orElseGet(() -> {
                    if (!promiseKeyFallbackEnabled) {
                        return null;
                    }
                    String fallbackKey = null;
                    if (encUserId != null && !encUserId.isBlank()) {
                        fallbackKey = promiseShareKeyRepository.findEncPromiseKey(promiseId, encUserId).orElse(null);
                    }
                    if (fallbackKey == null) {
                        fallbackKey = promiseShareKeyRepository
                                .findFirstByPromiseIdAndUserId(promiseId, userId)
                                .map(PromiseShareKey::getEncPromiseKey)
                                .orElse(null);
                    }
                    if (fallbackKey != null) {
                        promiseKeyLookupFallbackCounter.increment();
                        log.info("promisekey2 fallback hit: promiseId={}, lookupId={}, by={}",
                                promiseId, maskLookupId(lookupId), (encUserId != null && !encUserId.isBlank()) ? "encUserId" : "userId");
                    }
                    return fallbackKey;
                });

        if (encPromiseKey == null) {
            promiseKeyNotFoundCounter.increment();
            throw new PromiseNotFoundException(
                    BaseErrorCode.PROMISE_NOT_FOUND,
                    "[ERROR]: promiseId=" + promiseId + ", lookupId=" + maskLookupId(lookupId) + "에 해당하는 encPromiseKey를 찾을 수 없습니다."
            );
        }

        promiseKeyLookupSuccessCounter.increment();

        return new GetPromiseKey2(encPromiseKey);
    }

    private void validateLookup(String lookupId, Integer lookupVersion) {
        if (lookupVersion == null || lookupVersion != LOOKUP_VERSION_V1) {
            throw new PromiseLookupValidationException(
                    BaseErrorCode.BAD_REQUEST,
                    "[ERROR]: invalid lookupVersion=" + lookupVersion
            );
        }
        if (lookupId == null || !LOOKUP_ID_PATTERN.matcher(lookupId).matches()) {
            throw new PromiseLookupValidationException(
                    BaseErrorCode.BAD_REQUEST,
                    "[ERROR]: invalid lookupId format"
            );
        }
    }

    private String maskLookupId(String lookupId) {
        if (lookupId == null || lookupId.isBlank()) {
            return "********";
        }
        if (lookupId.length() < 8) {
            return "********";
        }
        String prefix = lookupId.substring(0, 8);
        return prefix + "****";
    }
}
