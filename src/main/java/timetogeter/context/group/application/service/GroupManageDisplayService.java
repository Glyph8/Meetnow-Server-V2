package timetogeter.context.group.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timetogeter.context.group.application.dto.request.ViewGroup2Request;
import timetogeter.context.group.application.dto.request.ViewGroup3Request;
import timetogeter.context.group.application.dto.response.*;
import timetogeter.context.group.exception.GroupIdNotFoundException;
import timetogeter.context.group.exception.GroupShareKeyNotFoundException;
import timetogeter.context.group.domain.entity.Group;
import timetogeter.context.group.domain.entity.GroupProxyUser;
import timetogeter.context.group.domain.repository.GroupProxyUserRepository;
import timetogeter.context.group.domain.repository.GroupRepository;
import timetogeter.context.group.domain.repository.GroupShareKeyRepository;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GroupManageDisplayService {
    private final GroupRepository groupRepository;
    private final GroupProxyUserRepository groupProxyUserRepository;
    private final GroupShareKeyRepository groupShareKeyRepository;

//======================
// 그룹 관리 - 그룹 메인 보기 (Step1,2,3)
//======================

    //그룹 관리 - 그룹 메인 보기 - step1 - 메인 서비스 메소드
    @Transactional
    public List<ViewGroup1Response> viewGroup1(String userId) {
        List<GroupProxyUser> groupProxyUserList = groupProxyUserRepository.findAllByUserId(userId);

        /*
        '개인키로 암호화된 그룹 아이디'-> encGroupId
        '개인키로 암호화한 (그룹키로 암호화한 사용자 고유 아이디)' -> encGroupMemberId
         */
        return groupProxyUserList.stream()
                .map(gpu -> new ViewGroup1Response(gpu.getEncGroupId(), gpu.getEncGroupMemberId()))
                .collect(Collectors.toList());
    }

    //그룹 관리 - 그룹 메인 보기 - step2 - 메인 서비스 메소드
    @Transactional
    public List<ViewGroup2Response> viewGroup2(List<ViewGroup2Request> requests) {
        List<ViewGroup2Response> responses = new ArrayList<>();

        for (ViewGroup2Request request : requests) {
            String groupId = request.groupId();
            String encGroupMemberId = request.encGroupMemberId();

            String encGroupKey = groupShareKeyRepository.findEncGroupKey(groupId, encGroupMemberId)
                    .orElseThrow(() -> new GroupShareKeyNotFoundException(
                            BaseErrorCode.GROUP_SHARE_KEY_NOT_FOUND,
                            "[ERROR]: 그룹 키 조회에 실패했습니다. groupId=" + groupId + ", encGroupMemberId=" + encGroupMemberId
                    ));

            responses.add(new ViewGroup2Response(encGroupKey));
        }

        return responses;
    }

    //그룹 관리 - 그룹 메인 보기 - step3 - 메인 서비스 메소드
    @Transactional
    public List<ViewGroup3Response> viewGroup3(List<ViewGroup3Request> requests) {
        List<ViewGroup3Response> result = new ArrayList<>();

        for (ViewGroup3Request request : requests) {
            String groupId = request.groupId();

            // 그룹 정보 조회
            Group group = groupRepository.findByGroupId(groupId)
                    .orElseThrow(() -> new GroupIdNotFoundException(BaseErrorCode.GROUP_ID_NOTFOUND, "[ERROR]: 존재하지 않는 그룹입니다: " + groupId));

            // 해당 그룹에 속한 사용자 encId 목록 조회 (groupKey로 암호화된 encUserId)
            List<String> encUserIdList = groupShareKeyRepository.findEncUserIdsByGroupId(groupId);

            // 응답 객체 생성
            ViewGroup3Response response = new ViewGroup3Response(
                    group.getGroupId(),
                    group.getGroupName(),
                    group.getGroupImg(),
                    group.getGroupInfo(),
                    group.getManagerId(),
                    encUserIdList
            );

            result.add(response);
        }

        return result;
    }

}