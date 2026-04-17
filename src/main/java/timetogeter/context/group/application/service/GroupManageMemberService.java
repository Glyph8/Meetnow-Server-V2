package timetogeter.context.group.application.service;

import com.amazonaws.Request;
import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import timetogeter.context.auth.domain.repository.UserRepository;
import timetogeter.context.auth.exception.UserNotFoundException;
import timetogeter.context.group.application.dto.request.*;
import timetogeter.context.group.application.dto.response.*;
import timetogeter.context.group.application.support.GroupLookupSupport;
import timetogeter.global.mail.EmailService;
import timetogeter.context.group.exception.GroupIdNotFoundException;
import timetogeter.context.group.exception.GroupInviteCodeExpired;
import timetogeter.context.group.exception.GroupProxyUserNotFoundException;
import timetogeter.context.group.exception.GroupShareKeyNotFoundException;
import timetogeter.context.group.domain.entity.Group;
import timetogeter.context.group.domain.entity.GroupProxyUser;
import timetogeter.context.group.domain.entity.GroupShareKey;
import timetogeter.context.group.domain.repository.GroupProxyUserRepository;
import timetogeter.context.group.domain.repository.GroupRepository;
import timetogeter.context.group.domain.repository.GroupShareKeyRepository;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GroupManageMemberService {
    @Autowired
    private AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.key}")
    private String s3Key;

    @Value("${group.lookup.fallback-enabled:true}")
    private boolean groupLookupFallbackEnabled;

    //@Value("${lambda.verify.url}")
    //private String lambdaVerifyUrl;

    private final GroupRepository groupRepository;
    private final GroupProxyUserRepository groupProxyUserRepository;
    private final GroupShareKeyRepository groupShareKeyRepository;
    private final UserRepository userRepository;

    private final GroupManageDisplayService groupManageDisplayService;

    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

//======================
// 그룹 상세 - 그룹 초대하기 (Step1,2,3)
//======================

    //그룹 상세 - 그룹 초대하기 - step1 - 메인 서비스 메소드
    @Transactional
    public InviteGroup1Response inviteGroup1(InviteGroup1Request request,String userId) {
        String groupId = request.groupId();
        boolean groupExists = groupRepository.findByGroupId(groupId).isPresent();
        if (!groupExists) {
            throw new GroupIdNotFoundException(
                    BaseErrorCode.GROUP_NOT_FOUND,
                    "[ERROR]: 존재하지 않는 그룹입니다: " + groupId
            );
        }

        Optional<GroupProxyUser> groupProxyUser = GroupLookupSupport.findGroupProxyUserWithFallback(
                groupProxyUserRepository,
                userId,
                groupId,
                request.lookupId(),
                request.lookupVersion(),
                request.encGroupId(),
                groupLookupFallbackEnabled,
                "/api/v1/group/invite1"
        );
        if (groupProxyUser.isEmpty()) {
            boolean hasLookup = GroupLookupSupport.hasLookup(request.lookupId(), request.lookupVersion());
            if (hasLookup) {
                boolean lookupExists = groupProxyUserRepository.existsByGroupIdAndLookup(
                        groupId,
                        request.lookupId(),
                        request.lookupVersion()
                );
                if (!lookupExists) {
                    throw new GroupProxyUserNotFoundException(
                            BaseErrorCode.LOOKUP_NOT_FOUND,
                            "[ERROR]: lookup 매핑이 존재하지 않습니다. groupId=" + groupId
                    );
                }
            }
            throw new GroupProxyUserNotFoundException(
                    BaseErrorCode.PROXY_USER_NOT_FOUND,
                    "[ERROR]: 해당 그룹 프록시 사용자 정보가 없습니다. groupId=" + groupId + ", userId=" + userId
            );
        }

        String encEncGroupMemberId = groupProxyUser.get().getEncGroupMemberId();

        return new InviteGroup1Response(encEncGroupMemberId);
    }

    //그룹 상세 - 그룹 초대하기 - step2 - 메인 서비스 메소드
    @Transactional
    public InviteGroup2Response inviteGroup2(InviteGroup2Request request) {
        //encUserId, groupId 로 GroupShareKey 요청
        String encUserId = request.encUserId();
        String groupId = request.groupId();

        String encGroupKey = groupShareKeyRepository.findEncGroupKey(groupId, encUserId)
                .orElseThrow(() -> new GroupShareKeyNotFoundException(
                        BaseErrorCode.GROUP_SHARE_KEY_NOT_FOUND,
                        "[ERROR]: 그룹 키 조회에 실패했습니다. groupId=" + groupId + ", encGroupMemberId=" + encUserId
                ));

        return new InviteGroup2Response(encGroupKey);
    }

    //그룹 상세 - 그룹 초대하기 - step3 - 메인 서비스 메소드
    @Transactional
    public InviteGroup3Response inviteGroup3(InviteGroup3Request request) {
        String encryptedValue = request.encryptedValue();
        String encryptedEmail = request.encryptedEmail();

        //randomkeyforredis redis에 TTL 설정해서 저장
        redisTemplate.opsForValue().set(
                "INVITE_KEY:" + encryptedValue + ":" + encryptedEmail,
                "0", //현재 이 코드로 시도된 횟수
                Duration.ofMinutes(120)
        );

        return new InviteGroup3Response(encryptedValue, "발급하신 초대코드가 redis에 저장되었습니다. (유효기한 : 120분)");
    }

//======================
// 그룹 상세 - 그룹 초대하기(간소화.ver)
//======================

    // 그룹 참가 URL 접속 후 로그인한 사용자의 이메일 반환
    public GetGroupJoinEmailResponse getGroupJoinEmail(String groupId, String userId) {
        // 그룹 존재 여부 확인
        groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: " + groupId));

        // 사용자 이메일 조회
        String email = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(BaseErrorCode.USER_NOT_FOUND, "[ERROR]: 존재하지 않는 유저입니다."))
                .getEmail();

        // 해당 이메일로 가입 링크 메일 전송
        String joinLinkUrl = "https://meetnow.duckdns.org/api/v1/group/member/save";
        String subject = "[MeetNow] 그룹 참여 링크 안내";
        String content = """
                안녕하세요,
                
                아래 링크를 통해 그룹 참여를 완료해 주세요.
                %s
                
                만약 본 메일을 요청하지 않으셨다면, 이 메일은 무시하셔도 됩니다.
                
                감사합니다.
                MeetNow 드림
                """.formatted(joinLinkUrl);
        emailService.sendPlainText(email, subject, content);

        return new GetGroupJoinEmailResponse("가입 링크를 이메일로 전송했어요."); // 이메일 전달 완료 메시지 반환
    }

    // 그룹 멤버 저장 - 가공된 정보들을 데이터베이스에 저장
    @Transactional
    public JoinGroupResponse saveGroupMember(SaveGroupMemberRequest request, String userId) {
        String groupId = request.groupId(); //그룹 아이디
        String encGroupKey = request.encGroupKey(); //개인키로 암호화한 그룹키
        String encUserId = request.encUserId(); //그룹키로 암호화한 사용자 고유 아이디
        String encGroupId = request.encGroupId(); //개인키로 암호화한 그룹 아이디
        String encencGroupMemberId = request.encencGroupMemberId(); //개인키로 암호화한 encUserId
        GroupLookupSupport.Lookup lookup = GroupLookupSupport.resolveLookupForWriteWithEndpoint(
                request.lookupId(), request.lookupVersion(), groupId, "/api/v1/group/member/save"
        );

        //GroupProxyUser에 저장
        groupProxyUserRepository.save(GroupProxyUser.of(
                userId,
                groupId,
                encGroupId,
                lookup.lookupId(),
                lookup.lookupVersion(),
                encencGroupMemberId,
                System.currentTimeMillis()
        ));

        //GroupShareKey에 저장
        groupShareKeyRepository.save(GroupShareKey.of(groupId, encUserId, encGroupKey));

        // response 구성
        Group joinedGroupName = groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: " + groupId));
        return new JoinGroupResponse(joinedGroupName.getGroupName() + "그룹에 참여 완료했어요.");
    }

//======================
// 그룹 관리 - 그룹 초대받기 (Step1)
//======================

    //그룹 관리 - 그룹 초대받기 - step1 - 메인 서비스 메소드
    @Transactional
    public JoinGroupResponse joinGroup(JoinGroupRequest request, String userId) {
        //1.유효한 초대코드인지 확인
        String encryptedValue = request.encryptedValue();
        Set<String> isValidKeys = redisTemplate.keys("INVITE_KEY:" + encryptedValue + ":*");
        if (isValidKeys == null || isValidKeys.isEmpty()) {
            throw new GroupInviteCodeExpired(BaseErrorCode.GROUP_INVITECODE_EXPIRED,
                    "[ERROR]: 초대코드가 만료되거나 유효하지 않습니다.");
        }
        String key = isValidKeys.iterator().next();

        //2. value = 시도 횟수 확인
        String attemptStr = redisTemplate.opsForValue().get(key);
        int attempt = (attemptStr != null) ? Integer.parseInt(attemptStr) : 0;
        if (attempt >= 5) {
            throw new GroupInviteCodeExpired(BaseErrorCode.GROUP_INVITECODE_EXPIRED,
                    "[ERROR]: 초대코드가 만료되거나 유효하지 않습니다.");
        }
        //시도 횟수 1 증가
        redisTemplate.opsForValue().increment(key);


        //3. 유효한 초대코드이면, userId와 초대대상이 같은지 확인
        // key에서 encryptedEmail 부분 추출
        String[] parts = key.split(":");
        String encryptedEmail = parts[parts.length - 1];

        // 복호화해서 실제 이메일 얻기
        String invitedEmail = decryptEmail(encryptedEmail, encryptedValue);

        // 현재 로그인한 유저의 이메일 조회
        String curUserEmail = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(BaseErrorCode.USER_NOT_FOUND, "[ERROR]: 존재하지 않는 유저입니다."))
                .getEmail();
        if (!curUserEmail.equals(invitedEmail)) {
            throw new GroupInviteCodeExpired(BaseErrorCode.GROUP_INVITECODE_EXPIRED,
                    "[ERROR]: 초대코드가 해당 사용자와 일치하지 않습니다.");
        }

        //4. ==== 초대코드 검증 성공후 데이터베이스에 저장
        String groupId = request.groupId(); //그룹 아이디
        String encGroupKey = request.encGroupKey(); //개인키로 암호화한 그룹키
        String encUserId = request.encUserId(); //그룹키로 암호화한 사용자 고유 아이디
        String encGroupId = request.encGroupId(); //개인키로 암호화한 그룹 아이디
        String encencGroupMemberId = request.encencGroupMemberId(); //개인키로 암호화한 encUserId
        GroupLookupSupport.Lookup lookup = GroupLookupSupport.resolveLookupForWriteWithEndpoint(
                request.lookupId(), request.lookupVersion(), groupId, "/api/v1/group/join"
        );

        //GroupProxyUser에 저장
        groupProxyUserRepository.save(GroupProxyUser.of(
                userId,
                groupId,
                encGroupId,
                lookup.lookupId(),
                lookup.lookupVersion(),
                encencGroupMemberId,
                System.currentTimeMillis()
        ));

        //GroupShareKey에 저장
        groupShareKeyRepository.save(GroupShareKey.of(groupId, encUserId, encGroupKey));

        //해당 키 사용 완료
        redisTemplate.delete(key);


        //5. response 구성
        Group joinedGroupName = groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: " + groupId));
        return new JoinGroupResponse(joinedGroupName.getGroupName() + "그룹에 참여 완료했어요.");
    }

    //그룹 관리 - 그룹 초대받기 - step1 - 서브 메소드
    private String decryptEmail(String encryptedEmail, String encryptedValue) {
        try {
            // 1) encryptedValue로 AES 키 만들기 (Node normalizeAESKey와 동일)
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hashedKey = sha256.digest(encryptedValue.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = new byte[32]; // 256비트
            System.arraycopy(hashedKey, 0, keyBytes, 0, 32);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // 2) IV (12바이트) → 암호화할 때 쓴 값과 동일해야 함
            // 네가 말한 대로 "전부 0"으로 통일
            byte[] iv = new byte[12];
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            // 3) Cipher 초기화
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // 4) base64 decode & 복호화
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedEmail);
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            // 5) 결과 문자열 반환
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("❌ 이메일 복호화 실패", e);
        }
    }

//======================
// 그룹 관리 - 그룹 나가기 (Step1,2)
//======================

    //그룹 관리 - 그룹 나가기 - step1 - 메인 서비스 메소드
    public LeaveGroup1Response leaveGroup1(LeavGroup1Request request, String userId) {
        GroupProxyUser groupProxyUser = GroupLookupSupport.findGroupProxyUserWithFallbackOrThrow(
                groupProxyUserRepository,
                userId,
                request.groupId(),
                request.lookupId(),
                request.lookupVersion(),
                request.encGroupId(),
                groupLookupFallbackEnabled,
                () -> new GroupProxyUserNotFoundException(
                        BaseErrorCode.GROUP_PROXY_USER_NOT_FOUND,
                        "[ERROR]: 해당 그룹 프록시 정보가 없습니다."
                ),
                "/api/v1/group/leave1"
        );

        Group group = groupRepository.findByGroupId(request.groupId())
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: "));

        boolean isManager = userId.equals(group.getManagerId());
        return new LeaveGroup1Response(request.groupId() ,
                group.getGroupName()+" 그룹에서 나가시겠어요?",
                isManager);
    }

    //그룹 관리 - 그룹 나가기 - step2 - 메인 서비스 메소드
    public LeaveGroup2Response leaveGroup2(LeaveGroup2Request request, String userId) {
        GroupProxyUser groupProxyUser = GroupLookupSupport.findGroupProxyUserWithFallbackOrThrow(
                groupProxyUserRepository,
                userId,
                request.groupId(),
                request.lookupId(),
                request.lookupVersion(),
                request.encGroupId(),
                groupLookupFallbackEnabled,
                () -> new GroupProxyUserNotFoundException(
                        BaseErrorCode.GROUP_PROXY_USER_NOT_FOUND,
                        "[ERROR]: 해당 그룹 프록시 정보가 없습니다."
                ),
                "/api/v1/group/leave2"
        );

        String encencGroupMemberId = groupProxyUser.getEncGroupMemberId();

        return new LeaveGroup2Response(encencGroupMemberId);
    }

    //그룹 관리 - 그룹 나가기 - step3 - 메인 서비스 메소드
    @Transactional
    public LeaveGroup3Response leaveGroup3(LeaveGroup3Request request) {
        boolean isManager = request.isManager();
        String message = "";
        Group group = groupRepository.findByGroupId(request.groupId())
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: "));
        String groupName = group.getGroupName();


        if (isManager){//매니저가 나가는 경우
            //request.groupId()에 해당하는 groupShareKeyRepository에서 다 지우기
            groupShareKeyRepository.deleteAllByGroupId(request.groupId());

            //TODO: groupProxyUser는 조금더 효과적으로 삭제하는 방법 찾는중

            message = groupName+" 그룹이 삭제되었어요.";
        }else {//일반 그룹원이 나가는 경우

            GroupProxyUser groupProxyUser = groupProxyUserRepository.findByEncGroupId(request.encencGroupMemberId())
                    .orElseThrow(() -> new GroupProxyUserNotFoundException(BaseErrorCode.GROUP_PROXY_USER_NOT_FOUND, "[ERROR]: 해당 그룹 프록시 정보가 없습니다"));

            GroupShareKey groupShareKey = groupShareKeyRepository.findByEncUserId(request.encUserId())
                    .orElseThrow(() -> new GroupShareKeyNotFoundException(BaseErrorCode.GROUP_SHARE_KEY_NOT_FOUND, "[ERROR]: 해당 그룹 쉐어 키 정보가 없습니다"));

            groupProxyUserRepository.delete(groupProxyUser);
            groupShareKeyRepository.delete(groupShareKey);
            message = groupName+" 그룹에서 나갔어요.";
        }

        return new LeaveGroup3Response(message);
    }
}
