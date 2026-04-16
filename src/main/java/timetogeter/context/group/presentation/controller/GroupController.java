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
import timetogeter.context.auth.domain.adaptor.UserPrincipal;
import org.springframework.web.bind.annotation.*;
import timetogeter.context.group.application.dto.request.*;
import timetogeter.context.group.application.dto.response.*;
import timetogeter.context.group.application.service.GroupManageDisplayService;
import timetogeter.context.group.application.service.GroupManageInfoService;
import timetogeter.context.group.application.service.GroupManageMemberService;
import timetogeter.global.interceptor.response.BaseResponse;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;
import timetogeter.global.interceptor.response.error.dto.SuccessResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "그룹", description = "그룹 생성, 그룹 정보 수정, 그룹 멤버 초대/나가기 API")
public class GroupController {

    private final GroupManageInfoService groupManageInfoService;
    private final GroupManageMemberService groupManageMemberService;
    private final GroupManageDisplayService groupManageDisplayService;

//======================
// 그룹 관리 - 그룹 메인 보기 (Step1,2,3)
//======================

    /*
    그룹 관리 - 그룹 메인 보기 - step1

    [웹] 그룹원이 userId를 담은 request를 요청 /api/v1/group/view1 ->
    [서버] GroupProxyUser의 userId에 해당하는 '개인키로 암호화된 그룹 아이디', '개인키로 암호화한
		 (그룹키로 암호화한 사용자 고유 아이디)'를 반환
     */
    @Operation(summary = "그룹 메인 보기 - Step1", description = """
        사용자 개인키로 암호화된 그룹 정보를 조회하는 단계입니다.

        - 요청: 사용자 인증 (UserPrincipal)
        - 처리: GroupProxyUser에서 userId 기반 그룹 정보 조회
        - 반환: 개인키로 암호화된 그룹 아이디와 그룹키로 암호화된 사용자 고유 아이디 리스트
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                        { "code": 401, "message": "인증이 필요합니다." }
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
    @GetMapping(value = "/view1", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<List<ViewGroup1Response>> viewGroup1(
            @AuthenticationPrincipal UserPrincipal userPrincipal) throws Exception{
        String userId = userPrincipal.getId();
        List<ViewGroup1Response> response = groupManageDisplayService.viewGroup1(userId);
        return new BaseResponse<>(response);
    }

    /*
    그룹 관리 - 그룹 메인 보기 - step2

    [웹] 암호화된 그룹 아이디, 암호화된 (그룹키로 암호화된 사용자 고유 아이디)를 개인키로 복호화 후
			그룹 아이디(리스트 형태로 그대로 저장해두기), (그룹키로 암호화된 사용자 고유 아이디)를 리스트 형태로 담아 요청
			/api/v1/group/view2 ->
    [서버] 그룹 아이디, (그룹키로 암호화한 사용자 고유 아이디)에 해당하는
			GroupShareKey테이블 내 "개인키로 암호화한 그룹키"를 리스트 형태로 넘김
     */
    @Operation(summary = "그룹 메인 보기 - Step2", description = """
        암호화된 그룹 ID와 그룹키로 암호화한 사용자 고유 아이디를 기반으로 그룹키 정보를 조회하는 단계입니다.

        - 요청: List<ViewGroup2Request> (암호화된 그룹 ID 및 사용자 ID)
        - 처리: GroupShareKey 테이블에서 개인키로 암호화한 그룹키 조회
        - 반환: List<ViewGroup2Response> (개인키로 암호화된 그룹키)
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹키 조회 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/view2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<List<ViewGroup2Response>> viewGroup2(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<ViewGroup2Request> requests) throws Exception{
        List<ViewGroup2Response> response = groupManageDisplayService.viewGroup2(requests);
        return new BaseResponse<>(response);
    }

    /*
    그룹 관리 - 그룹 메인 보기 - step3

    [웹] 개인키로 "개인키로 암호화한 그룹키" 복호화후 저장해 두었다가,
		이전 저장한 그룹 아이디를 GroupShareKey테이블 내 레코드 리스트 요청, 그룹 정보 요청 /api/v1/group/view3
		->
    [서버] 해당 그룹 아이디에 해당하는 레코드들 리스트로 반환 , <그룹 정보> 또한 리스트 형태로 반환
     */
    @Operation(summary = "그룹 메인 보기 - Step3", description = """
        개인키로 암호화된 그룹키를 이용하여 그룹 레코드 및 그룹 정보를 조회하는 단계입니다.

        - 요청: List<ViewGroup3Request> (이전 단계에서 저장한 그룹 ID 및 그룹키)
        - 처리: 해당 그룹 ID에 대한 레코드 및 그룹 정보 조회
        - 반환: List<ViewGroup3Response> (그룹 정보 및 레코드 리스트)
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 레코드 및 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/view3", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<List<ViewGroup3Response>> viewGroup3(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<ViewGroup3Request> requests) throws Exception{
        List<ViewGroup3Response> response = groupManageDisplayService.viewGroup3(requests);
        return new BaseResponse<>(response);
    }

//======================
// 그룹 관리 - 그룹 만들기 (Step1,2)
//======================

    /*
    그룹 관리 - 그룹 만들기 - step1

    [웹] (예비 방장)그룹원이 Group정보를 담은 request를 요청 /api/v1/group/new1 ->
    [서버] Group에 request기반으로 저장, 저장한 후 생성된 Group의 아이디 프론트에 반환 ->
     */
    @Operation(summary = "그룹 만들기 - Step1", description = """
        예비 방장이 그룹 정보를 생성하는 단계입니다.

        - 요청: 그룹 정보(CreateGroup1Request)
        - 처리: Group 테이블에 요청 기반으로 그룹 저장
        - 반환: 생성된 Group의 아이디를 포함한 CreateGroup1Response
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 생성 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 (필드 누락/유효성 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "그룹 이름 누락", value = """
                                    { "code": 400, "message": "groupName은 필수입니다." }
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
    @PostMapping(value = "/new1", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<CreateGroup1Response> createGroup1(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CreateGroup1Request request) throws Exception{
        String userId = userPrincipal.getId();
        CreateGroup1Response response = groupManageInfoService.createGroup1(request,userId);
        return new BaseResponse<>(response);
    }

    /*
    그룹 관리 - 그룹 만들기 - step2

    [웹] 그룹키 자체적 생성후,
         GroupId와
         Group 아이디를 개인키로 암호화 한 것,
		 그룹키로 암호화한 사용자 아이디,
		 (그룹키로 암호화한 사용자 아이디)를 개인키로 암호화한 것,
		 개인키로 암호화한 그룹키 5개를 묶어 request로 요청 /api/v1/group/new2 ->
    [서버] 요청을 GroupProxyUser, Group, GroupShareKey에 저장
     */

    @Operation(summary = "그룹 만들기 - Step2", description = """
        그룹 키 및 암호화 정보를 포함하여 그룹을 최종 생성하는 단계입니다.

        - 요청: GroupId, 개인키/그룹키로 암호화한 사용자 아이디 등(CreateGroup2Request)
        - 처리: GroupProxyUser, Group, GroupShareKey 테이블에 저장
        - 반환: 생성 결과(CreateGroup2Response)
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 생성 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 (필드 누락/유효성 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "필수 필드 누락", value = """
                                    { "code": 400, "message": "groupId 또는 encUserId 등 필수 필드가 누락되었습니다." }
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
    @PostMapping(value = "/new2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<CreateGroup2Response> createGroup2(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid CreateGroup2Request request) throws Exception{
        String userId = userPrincipal.getId();
        CreateGroup2Response response = groupManageInfoService.createGroup2(request,userId);
        return new BaseResponse(response);
    }

//======================
// 그룹 관리 - 그룹 나가기
//======================

    /*
    그룹 관리 - 나가기 //TODO: 약속 테이블 관련 내용도 사라져야함
    */


}
