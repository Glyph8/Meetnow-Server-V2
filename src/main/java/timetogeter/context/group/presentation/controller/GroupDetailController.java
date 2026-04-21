package timetogeter.context.group.presentation.controller;

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
import timetogeter.context.group.application.dto.request.*;
import timetogeter.context.group.application.dto.response.*;
import timetogeter.context.group.application.service.GroupManageDisplayService;
import timetogeter.context.group.application.service.GroupManageInfoService;
import timetogeter.context.group.application.service.GroupManageMemberService;
import timetogeter.global.interceptor.response.BaseResponse;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "그룹", description = "그룹 생성, 그룹 정보 수정, 그룹 멤버 초대/나가기 API")
public class GroupDetailController {
    private final GroupManageInfoService groupManageInfoService;
    private final GroupManageMemberService groupManageMemberService;
    private final GroupManageDisplayService groupManageDisplayService;

//======================
// 그룹 상세 - 그룹 정보 수정 (Step1,2,3)
//======================
    /*
    그룹 상세 - 그룹 정보 수정 - step1

    [웹] 방장이 userId와 개인키로 암호화한 그룹 아이디(encGroupId), 그룹 아이디,
			수정하려는 request를 요청
			/api/v1/group/edit1 ->
    [서버] Group내 groupId 레코드들중 managerId와 userId가 동등한게 있는 경우,
			수정하려는 request반영해서 저장.
			GroupProxyUser테이블 내에서 userId,encGroupId에 해당하는
			encencGroupMemberId반환
    */
    @Operation(summary = "그룹 정보 수정 - Step1", description = """
            방장이 그룹 정보를 수정하는 단계입니다.

            - 요청: 방장이 userId, groupId, lookupId, lookupVersion, 수정하려는 request
            - lookup 계약: lookupId(64-char hex), lookupVersion(현재 1) 필수
            - encGroupId는 fallback 호환 필드(옵션)
            - 처리: Group 내 managerId == userId 인 경우 수정 요청을 반영하여 저장
            - 반환: GroupProxyUser 테이블에서 userId, encGroupId 에 해당하는 encencGroupMemberId
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),

            @ApiResponse(responseCode = "400", description = "요청 형식 오류 (필드 누락/유효성 실패, LOOKUP_INVALID_FORMAT/LOOKUP_VERSION_UNSUPPORTED/LOOKUP_LEGACY_FALLBACK_DISABLED)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "groupId 누락", value = """
                                        { "code": 400, "message": "groupId는 필수입니다." }
                                        """),
                                    @ExampleObject(name = "lookupId 형식 오류", value = """
                                        { "code": 400, "message": "lookupId 형식이 올바르지 않아요" }
                                        """)
                            }
                    )),

            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 401, "message": "인증이 필요합니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "403", description = "권한 없음 (방장이 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 403, "message": "해당 그룹을 수정할 권한이 없습니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "404", description = "리소스 없음 (그룹 없음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 404, "message": "존재하지 않는 groupId 입니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "422", description = "복호화/무결성 오류 (encGroupId가 유효하지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 422, "message": "encGroupId 복호화 결과가 유효하지 않습니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 500, "message": "서버 내부 오류가 발생했습니다." }
                                """)
                    ))
    })
    @PostMapping(value = "/edit1", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<EditGroup1Response> editGroup1(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid EditGroup1Request request) throws Exception{
        String managerId = userPrincipal.getId();
        EditGroup1Response response = groupManageInfoService.editGroup1(request, managerId);
        return new BaseResponse<>(response);
    }

    /*
    그룹 상세 - 그룹 정보 수정 - step2

    [웹] encencGroupMemberId를 개인키로 복호화한 후(encUserId = encGroupMemberId),
		groupId, encUserId를 보냄 /api/v1/group/edit2->
    [서버] GroupShareKey테이블 내에서 groupId, encUserId에 해당하는
			레코드의 encGroupKey를 반환
    */
    @Operation(summary = "그룹 정보 수정 - Step2", description = """
            - 요청: 클라이언트가 encencGroupMemberId 를 복호화해 얻은 encUserId 와 groupId 전송
            - 처리: GroupShareKey 테이블에서 groupId, encUserId 에 해당하는 encGroupKey 반환
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "encGroupKey 반환 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(필드 누락/형식 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "필드 누락",
                                            value = """
                                                { "code": 400, "message": "groupId는 필수입니다." }
                                                """
                                    )
                            }
                    )),

            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 401, "message": "인증이 필요합니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "403", description = "권한 없음(해당 그룹 접근 권한 없음)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 403, "message": "해당 그룹에 대한 접근 권한이 없습니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "404", description = "리소스 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "그룹 없음",
                                            value = """
                                                { "code": 404, "message": "존재하지 않는 groupId 입니다." }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "키 매핑 없음",
                                            value = """
                                                { "code": 404, "message": "groupId와 encUserId에 해당하는 encGroupKey가 없습니다." }
                                                """
                                    )
                            }
                    )),

            @ApiResponse(responseCode = "409", description = "충돌(매핑 불일치)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 409, "message": "encUserId가 해당 groupId의 멤버와 일치하지 않습니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "422", description = "비즈니스 검증 실패(복호화/무결성 오류 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 422, "message": "encUserId 복호화 결과가 유효하지 않습니다." }
                                """)
                    )),

            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                { "code": 500, "message": "서버 내부 오류가 발생했습니다." }
                                """)
                    ))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/edit2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<EditGroup2Response> editGroup2(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody EditGroup2Request request) throws Exception{
        EditGroup2Response response = groupManageInfoService.editGroup2(request);
        return new BaseResponse<>(response);
    }

    /*
    그룹 상세 - 그룹 정보 수정 - step3

    [웹] 개인키로 encGroupKey를 복호화한 후 그룹키 저장, 그룹 아이디 보냄
			/api/v1/group/edit3 ->
    [서버] GroupShareKey테이블 내 groupId로 찾은
			encUserId 리스트, 그룹 아이디에 따른 그룹 정보 반환
    */
    @Operation(summary = "그룹 정보 수정 - Step3", description = """
            - 요청: 클라이언트가 개인키로 encGroupKey 를 복호화한 후 그룹키 저장 및 groupId 전송
            - 처리: GroupShareKey 테이블에서 해당 groupId로 encUserId 리스트 조회 후 그룹 정보 반환
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 groupId 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "복호화 실패 등 비즈니스 검증 에러",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/edit3", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<EditGroup3Response> editGroup3(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody EditGroup3Request request) throws Exception{
        EditGroup3Response response = groupManageInfoService.editGroup3(request);
        return new BaseResponse<>(response);
    }

}
