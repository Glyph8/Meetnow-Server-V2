package timetogeter.context.group.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timetogeter.context.group.application.dto.request.*;
import timetogeter.context.group.application.dto.response.*;
import timetogeter.context.group.application.support.GroupLookupSupport;
import timetogeter.context.group.exception.GroupIdNotFoundException;
import timetogeter.context.group.exception.GroupManagerMissException;
import timetogeter.context.group.exception.GroupProxyUserNotFoundException;
import timetogeter.context.group.exception.GroupShareKeyNotFoundException;
import timetogeter.context.group.domain.entity.Group;
import timetogeter.context.group.domain.entity.GroupProxyUser;
import timetogeter.context.group.domain.entity.GroupShareKey;
import timetogeter.context.group.domain.repository.GroupProxyUserRepository;
import timetogeter.context.group.domain.repository.GroupRepository;
import timetogeter.context.group.domain.repository.GroupShareKeyRepository;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GroupManageInfoService {
    private final GroupManageDisplayService groupManageDisplayService;

    private final GroupRepository groupRepository;
    private final GroupProxyUserRepository groupProxyUserRepository;
    private final GroupShareKeyRepository groupShareKeyRepository;
    @Value("${group.lookup.fallback-enabled:true}")
    private boolean groupLookupFallbackEnabled;

//======================
// 그룹 관리 - 그룹 만들기 (Step1,2)
//======================

    //그룹 관리 - 그룹 만들기 - step1 - 메인 서비스 메소드
    @Transactional
    public CreateGroup1Response createGroup1(CreateGroup1Request request, String managerId) {
        //Group 테이블에 저장
        Group group = Group.of(request.groupName(), request.groupImg(), request.groupExplain(), managerId);
        groupRepository.save(group);

        //그룹 아이디 반환
        return new CreateGroup1Response(group.getGroupId());
    }

    //그룹 관리 - 그룹 만들기 - step2 - 메인 서비스 메소드
    @Transactional
    public CreateGroup2Response createGroup2(CreateGroup2Request request, String userId) {
        String groupId = request.groupId(); //그룹 아이디
        String encGroupId = request.encGroupId(); //개인키로 암호화한 그룹 아이디
        String encencGroupMemberId = request.encencGroupMemberId(); //개인키로 암호화한 (그룹키로 암호화한 사용자 고유 아이디)
        String encUserId = request.encUserId(); //그룹키로 암호화한 사용자 고유 아이디
        String encGroupKey = request.encGroupKey(); //개인키로 암호화한 그룹키
        GroupLookupSupport.Lookup lookup = GroupLookupSupport.resolveLookupForWrite(
                request.lookupId(), request.lookupVersion(), groupId, "/api/v1/group/new2"
        );

        //GroupProxyUser테이블 내 저장
        groupProxyUserRepository.save(GroupProxyUser.of(
                userId,
                groupId,
                encGroupId,
                lookup.lookupId(),
                lookup.lookupVersion(),
                encencGroupMemberId,
                System.currentTimeMillis()
        ));
        //GroupShareKey테이블 내 저장
        groupShareKeyRepository.save(GroupShareKey.of(groupId, encUserId, encGroupKey));

        Group groupFound = groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: " + groupId));

        // 응답 반환
        return new CreateGroup2Response(
                groupFound.getGroupId(),
                groupFound.getGroupName(),
                groupFound.getGroupInfo(),
                groupFound.getGroupImg(),
                groupFound.getManagerId()
        );

    }

//======================
// 그룹 상세 - 그룹 정보 수정 (Step1,2,3)
//======================

    //그룹 상세 - 그룹 정보 수정 - step1 - 메인 서비스 메소드
    @Transactional
    public EditGroup1Response editGroup1(EditGroup1Request request, String managerId) {
        String groupId = request.groupId();
        /*
        Group내 groupId 레코드들중 managerId와 userId가 동등한게 있는 경우,
		수정하려는 request반영해서 groupRepository에 저장.
         */
        boolean isManager = groupRepository.existsByGroupIdAndManagerId(groupId, managerId);
        if (!isManager){
            throw new GroupManagerMissException(BaseErrorCode.NOT_GROUP_MANAGER_ERROR, "[ERROR]: "+ managerId +"는 방장이 아닙니다.");
        }

        //groupId와 managerId가 일치하므로, 수정하려는 그룹 상세 정보 반영
        Group groupFound = groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: " + groupId));
        groupFound.update(request);

        GroupProxyUser groupProxyUser = GroupLookupSupport.findGroupProxyUserWithFallbackOrThrow(
                groupProxyUserRepository,
                managerId,
                groupId,
                request.lookupId(),
                request.lookupVersion(),
                request.encGroupId(),
                groupLookupFallbackEnabled,
                () -> new GroupProxyUserNotFoundException(
                        BaseErrorCode.GROUP_PROXY_USER_NOT_FOUND,
                        "[ERROR]: 해당 유저의 그룹 프록시 정보가 없습니다."
                ),
                "/api/v1/group/edit1"
        );
        String encEncGroupMemberId = groupProxyUser.getEncGroupMemberId();

        return new EditGroup1Response(encEncGroupMemberId);

    }

    //그룹 상세 - 그룹 정보 수정 - step2 - 메인 서비스 메소드
    @Transactional
    public EditGroup2Response editGroup2(EditGroup2Request request) {
        String groupId = request.groupId();
        String encUserId = request.encUserId();

        // groupId, encUserId에 해당하는 encGroupKey 조회
        String encGroupKey = groupShareKeyRepository.findEncGroupKey(groupId, encUserId)
                .orElseThrow(() -> new GroupShareKeyNotFoundException(
                        BaseErrorCode.GROUP_SHARE_KEY_NOT_FOUND,
                        "[ERROR]: 그룹 키 조회에 실패했습니다. groupId=" + groupId + ", encGroupMemberId=" + encUserId
                ));

        return new EditGroup2Response(encGroupKey);
    }

    //그룹 상세 - 그룹 정보 수정 - step3 - 메인 서비스 메소드
    @Transactional
    public EditGroup3Response editGroup3(EditGroup3Request request) {
        String groupId = request.groupId();

        //그룹 유저들
        List<GroupShareKey> groupShareKeyList = groupShareKeyRepository.findAllByGroupId(groupId);
        List<String> encUserIdList = groupShareKeyList.stream()
                .map(GroupShareKey::getEncUserId)
                .toList();

        //그룹 정보
        Group group = groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GroupIdNotFoundException(
                        BaseErrorCode.GROUP_ID_NOTFOUND,
                        "[ERROR]: 존재하지 않는 그룹입니다: " + groupId
                ));

        return new EditGroup3Response(
                group.getGroupName(),
                group.getGroupInfo(),
                group.getGroupImg(),
                encUserIdList
        );
    }
}
