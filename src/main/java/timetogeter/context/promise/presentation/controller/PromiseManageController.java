package timetogeter.context.promise.presentation.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timetogeter.context.auth.domain.adaptor.UserPrincipal;
import timetogeter.context.promise.application.dto.request.manage.*;
import timetogeter.context.promise.application.dto.response.manage.*;
import timetogeter.context.promise.application.service.PromiseManageInfoService;
import timetogeter.global.interceptor.response.BaseResponse;

@RestController
@RequestMapping("/promise")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "약속", description = "약속 생성, 약속 확인 , 약속 멤버 초대/참여하기 API")
public class PromiseManageController {
    private final PromiseManageInfoService promiseManageInfoService;

//======================
// 약속 만들기 - 약속 만들고 초대하기 (Step1)
//======================
    /*
    약속 만들기 - 초대하기 Step1

    [웹] encGroupId(groupId를 개인키)로 암호화해서 groupId, encGroupId 요청
        /promise/invite1 ->
    [서버] GroupProxyUser테이블 내에서 encencGroupMemberId 반환
     */
@Operation(
        summary = "약속 초대 - Step1",
        description = """
        사용자를 약속에 초대합니다.

        - 요청: 사용자 인증 (UserPrincipal) + InvitePromise1Request
        - 처리: GroupProxyUser 테이블에서 encGroupMemberId 조회 후 반환
        - 반환: encGroupKey 리스트 (InvitePromise1Response)
        """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "약속 초대 성공",
                content = @Content(
                        schema = @Schema(implementation = InvitePromise1Response.class),
                        examples = @ExampleObject(value = """
                            {
                                "whichEmailIn": ["user1@example.com", "user2@example.com"],
                                "message": "가입 링크를 이메일로 전송했어요."
                            }
                        """)
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "초대 요청 실패",
                content = @Content(
                        schema = @Schema(implementation = BaseResponse.class),
                        examples = @ExampleObject(value = """
                            { "code": 400, "message": "초대 요청이 유효하지 않습니다." }
                        """)
                )
        ),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content(
                        schema = @Schema(implementation = BaseResponse.class),
                        examples = @ExampleObject(value = """
                            { "code": 401, "message": "인증이 필요합니다." }
                        """)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = @Content(
                        schema = @Schema(implementation = BaseResponse.class),
                        examples = @ExampleObject(value = """
                            { "code": 500, "message": "서버 내부 오류가 발생했습니다." }
                        """)
                )
        )
})
@SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/invite1", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<InvitePromise1Response> invitePromise1(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody InvitePromise1Request request) throws Exception{
        InvitePromise1Response response = promiseManageInfoService.invitePromise1(request);
        return new BaseResponse<>(response);
    }

//======================
// 약속 만들기 - 참여하기 (Step1)
//======================
    /*
     약속 만들기 - 참여하기 Step1

    [웹] encencGroupMemberId를 개인키로 복호화(encUserId)한후, groupId, encUserId 요청
        /promise/join1 ->
    [서버] encGroupKey 반환
    [웹] 개인키로 encGroupKey복호화해서 groupKey얻음
    */
@Operation(
        summary = "약속 참여 - Step1",
        description = """
        사용자가 약속에 참여합니다.

        - 요청: 사용자 인증 (UserPrincipal) + JoinPromise1Request
        - 처리: 기존 암호문 저장 + lookupId/lookupVersion 기반 참여키 저장
        - 예외: 동일 lookup에 다른 encPromiseKey 요청 시 409
        """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "약속 참여 성공",
                content = @Content(
                        schema = @Schema(implementation = JoinPromise1Response.class),
                        examples = @ExampleObject(value = """
                            { "message": "약속에 참여하였습니다." }
                        """)
                )
        ),
        @ApiResponse(
                responseCode = "409",
                description = "기존 참여 데이터와 키 불일치",
                content = @Content(
                        schema = @Schema(implementation = BaseResponse.class),
                        examples = @ExampleObject(value = """
                            { "code": 409, "message": "약속키 데이터가 일치하지 않아요" }
                        """)
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "참여 횟수 초과",
                content = @Content(
                        schema = @Schema(implementation = BaseResponse.class),
                        examples = @ExampleObject(value = """
                            { "code": 400, "message": "약속 참여 시도 횟수가 초과되었습니다." }
                        """)
                )
        ),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content(
                        schema = @Schema(implementation = BaseResponse.class),
                        examples = @ExampleObject(value = """
                            { "code": 401, "message": "인증이 필요합니다." }
                        """)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = @Content(
                        schema = @Schema(implementation = BaseResponse.class),
                        examples = @ExampleObject(value = """
                            { "code": 500, "message": "서버 내부 오류가 발생했습니다." }
                        """)
                )
        )
})
@SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/join1", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<JoinPromise1Response> joinPromise1(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid JoinPromise1Request request) throws Exception{
        String userId = userPrincipal.getId();
        JoinPromise1Response response = promiseManageInfoService.joinPromise1(userId, request);
        return new BaseResponse<>(response);
    }


}
