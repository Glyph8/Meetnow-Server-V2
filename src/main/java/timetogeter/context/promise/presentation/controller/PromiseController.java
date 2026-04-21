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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import timetogeter.context.auth.domain.adaptor.UserPrincipal;
import timetogeter.context.promise.application.dto.request.DispersePromiseReqDTO;
import timetogeter.context.promise.application.dto.request.ExitPromiseReqDTO;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise1Request;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise2Request;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise3Request;
import timetogeter.context.promise.application.dto.request.basic.CreatePromise4Request;
import timetogeter.context.promise.application.dto.request.manage.GetPromiseRequest;
import timetogeter.context.promise.application.dto.response.UserInfoListResDTO;
import timetogeter.context.promise.application.dto.response.UserInfoResDTO;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise1Response;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise2Response;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise3Response;
import timetogeter.context.promise.application.dto.response.basic.CreatePromise4Response;
import timetogeter.context.promise.application.dto.response.manage.GetPromiseKey1;
import timetogeter.context.promise.application.dto.response.manage.GetPromiseKey2;
import timetogeter.context.promise.application.service.PromiseManageInfoService;
import timetogeter.context.promise.application.service.PromiseSecurityService;
import timetogeter.context.promise.application.dto.response.UserIdsResDTO;
import timetogeter.global.interceptor.response.BaseCode;
import timetogeter.global.interceptor.response.BaseResponse;
import timetogeter.global.interceptor.response.error.dto.ErrorResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/promise")
@Tag(name = "약속", description = "약속 생성, 약속 확인, 약속 멤버 초대/참여하기/나가기 API")
@SecurityRequirement(name = "BearerAuth")
public class PromiseController {

    private final PromiseSecurityService promiseSecurityService;
    private final PromiseManageInfoService promiseManageInfoService;

    @Operation(summary = "암호화된 약속원들의 아이디 조회", description = "암호화된 약속원들의 아이디를 조회한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = UserIdsResDTO.class)))
    })
    @GetMapping("/mem/s1/{promiseId}")
    public BaseResponse<UserIdsResDTO> getUsersByPromiseTime1(@PathVariable("promiseId") String promiseId) {
        UserIdsResDTO dto = promiseSecurityService.getUsersByPromiseTime(promiseId);
        return new BaseResponse<>(dto);
    }

    @Operation(summary = "약속원 정보 조회(약속아이디)", description = "약속원에 대한 정보를 조회한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResDTO.class)))
    })
    @PostMapping("/mem/s2/{promiseId}")
    public BaseResponse<UserInfoResDTO> getUsersByPromiseTime2(@PathVariable("promiseId") String promiseId,
                                                       @RequestBody UserIdsResDTO reqDTO) {
        UserInfoResDTO dto = promiseSecurityService.getUserInfoByDTO(promiseId, reqDTO);
        return new BaseResponse<>(dto);
    }

    @Operation(summary = "약속원 정보 조회(전체)", description = "약속원에 대한 정보를 조회한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = UserInfoListResDTO.class)))
    })
    @PostMapping("/mem/s2")
    public BaseResponse<UserInfoListResDTO> getUsersByPromiseTime3(@RequestBody UserIdsResDTO reqDTO) {
        UserInfoListResDTO dto = promiseSecurityService.getUserInfoByDTO2(reqDTO);
        return new BaseResponse<>(dto);
    }

    @Operation(summary = "약속 나가기", description = "약속을 나갈 경우 약속관련 테이블을 삭제한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    })
    @DeleteMapping("/exit")
    public BaseResponse<Object> exitPromise(@RequestBody ExitPromiseReqDTO reqDTO) {
        promiseSecurityService.exitPromise(reqDTO);
        return new BaseResponse<>(BaseCode.SUCCESS_EXIT_PROMISE);
    }

    @Operation(summary = "약속 해산하기", description = "약속을 해산할 경우 약속관련 테이블을 모두 삭제한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    })
    @DeleteMapping("/disperse")
    public BaseResponse<Object> dispersePromise(@RequestBody DispersePromiseReqDTO reqDTO) {
        promiseSecurityService.dispersePromise(reqDTO);
        return new BaseResponse<>(BaseCode.SUCCESS_DISPERSE_PROMISE);
    }


//======================
// 약속 만들기 - 기본 정보 입력 (Step1,2,3,4)
//======================

    /*
    약속 만들기 - 기본 정보 입력 Step1

    [웹] 그룹원이 userId, encGroupId(개인키로 암호화한 groupId)를 담은 request를 요청
        /promise/create1 ->
    [서버] GroupProxyUser의 userId에 해당하는 '개인키로 암호화된 그룹 아이디', '개인키로 암호화한
		  (그룹키로 암호화한 사용자 고유 아이디)'를 리스트 형태로 반환
     */
    @Operation(summary = "약속 만들기 - Step1", description = """
        사용자 개인키로 암호화된 그룹 정보를 조회하는 단계입니다.

        - 요청: 사용자 인증 (UserPrincipal) + CreatePromise1Request
        - 처리: GroupProxyUser에서 userId 기반 그룹 정보 조회
        - 반환: 개인키로 암호화된 그룹 아이디와 그룹키로 암호화된 사용자 고유 아이디 리스트
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = CreatePromise1Response.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 401, \"message\": \"인증이 필요합니다.\" }")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\" }")
                    ))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/create1", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<CreatePromise1Response> createPromise1(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid CreatePromise1Request request) throws Exception{
        String userId = userPrincipal.getId();
        CreatePromise1Response response = promiseManageInfoService.createPromise1(userId,request);
        return new BaseResponse<>(response);
    }

    /*
    약속 만들기 - 기본 정보 입력 Step2

    [웹] 개인키로 암호화된 그룹 아이디, 암호화된 (그룹키로 암호화된 사용자 고유 아이디) 복호화 후
			그룹 아이디(저장해두기), (그룹키로 암호화된 사용자 고유 아이디)를 리스트 형태로 담아 요청
			/promise/create2 ->
    [서버] 그룹 아이디, (그룹키로 암호화한 사용자 고유 아이디)에 해당하는
			GroupShareKey테이블 내 "개인키로 암호화한 그룹키" 넘김
    */
    @Operation(summary = "약속 만들기 - Step2", description = """
        개인키로 암호화된 그룹 아이디와 사용자 고유 아이디를 복호화하여 GroupShareKey 정보를 조회합니다.

        - 요청: 사용자 인증 (UserPrincipal) + CreatePromise2Request
        - 처리: 그룹 아이디, 사용자 고유 아이디 기준으로 GroupShareKey 조회
        - 반환: 개인키로 암호화한 그룹키 리스트
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GroupShareKey 조회 성공",
                    content = @Content(schema = @Schema(implementation = CreatePromise2Response.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/create2", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<CreatePromise2Response> createPromise2(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CreatePromise2Request request) throws Exception{
        String userId = userPrincipal.getId();
        CreatePromise2Response response = promiseManageInfoService.createPromise2(userId,request);
        return new BaseResponse<>(response);
    }

    /*
    약속 만들기 - 기본 정보 입력  Step3

    [웹] 개인키로 "개인키로 암호화한 그룹키" 복호화후 저장해 두었다가,
		이전 저장한 그룹 아이디를 GroupShareKey테이블 내 레코드 리스트 요청
		 /promise/create3
    [서버] 해당 그룹 아이디에 해당하는 레코드들 리스트로 반환 (그룹 내 그룹원들 볼 수 있음)
    */
    @Operation(summary = "약속 만들기 - Step3", description = """
        개인키로 암호화한 그룹키를 복호화 후 이전 저장한 그룹 아이디 기준으로 GroupShareKey 레코드 리스트 조회.

        - 요청: CreatePromise3Request
        - 처리: 그룹 내 그룹원 정보 조회
        - 반환: 그룹원 리스트
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹원 조회 성공",
                    content = @Content(schema = @Schema(implementation = CreatePromise3Response.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/create3", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<CreatePromise3Response> createPromise3(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CreatePromise3Request request) throws Exception{
        CreatePromise3Response response = promiseManageInfoService.createPromise3(request);
        return new BaseResponse<>(response);
    }

    /*
    약속 만들기 - 기본 정보 입력  Step4

    [웹] 개인키로 "개인키로 암호화한 그룹키" 복호화후 저장해 두었다가,
		이전 저장한 그룹 아이디를 GroupShareKey테이블 내 레코드 리스트 요청
		 /promise/create4
    [서버] 해당 그룹 아이디에 해당하는 레코드들 리스트로 반환 (그룹 내 그룹원들 볼 수 있음)
    */
    @Operation(summary = "약속 만들기 - Step4", description = """
        개인키로 암호화한 그룹키를 복호화 후 이전 저장한 그룹 아이디 기준으로 GroupShareKey 레코드 리스트 조회 (최종 단계).

        - 요청: 사용자 인증 (UserPrincipal) + CreatePromise4Request
        - 처리: 그룹 내 그룹원 정보 조회
        - 반환: 그룹원 리스트
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹원 조회 성공",
                    content = @Content(schema = @Schema(implementation = CreatePromise4Response.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/create4", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<CreatePromise4Response> createPromise4(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CreatePromise4Request request) throws Exception{
        String userId = userPrincipal.getId();
        CreatePromise4Response response = promiseManageInfoService.createPromise4(userId,request);
        return new BaseResponse<>(response);
    }

    /*
    promisekey를 획득하는 과정 - step1

    promise_proxy_user 테이블에 있는 enc_promise_id (개인키로 암호화한 promise_id) 리스트 반환
     */
    @Operation(summary = "promisekey를 획득하는 과정 - step1", description = """
         promise_proxy_user 테이블에 있는 enc_promise_id (개인키로 암호화한 promise_id) 리스트 반환
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "enc_promise_id 리스트 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPromiseKey1.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 401, \"message\": \"인증이 필요합니다.\" }")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\" }")
                    ))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/promisekey1", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<GetPromiseKey1> getPromiseKey1(
            @AuthenticationPrincipal UserPrincipal userPrincipal) throws Exception{
        String userId = userPrincipal.getId();
        GetPromiseKey1 response = promiseManageInfoService.getPromiseKey1(userId);
        return new BaseResponse<>(response);
    }

    /*
    promisekey를 획득하는 과정 - step2

    encPromiseIdList 의 각 원소들을 개인키로 복호화한후, 요청하고 싶은 promise_id 고르기.
    요청 : promiseId를 요청
    응답 : promise_id에 해당하는 enc_promise_key (개인키로 암호화한 promise_key) 반환받음
     */
    @Operation(summary = "promisekey를 획득하는 과정 - step2", description = """
          encPromiseIdList의 각 원소들을 개인키로 복호화한 후, 요청하고 싶은 promise_id를 고르기.
          
          - 요청: promiseId, lookupId, lookupVersion(필수), encUserId(optional, 호환기간 fallback)
          - 응답: promise_id에 해당하는 enc_promise_key (개인키로 암호화한 promise_key) 반환받음
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "enc_promise_key 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPromiseKey2.class))),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류(lookupId/lookupVersion 검증 실패 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 400, \"message\": \"잘못된 URL 요청이에요. 요청 포맷을 확인해주세요\" }")
                    )),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 403, \"message\": \"접근 권한이 없어요\" }")
                    )),
            @ApiResponse(responseCode = "404", description = "약속 또는 약속키 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 404, \"message\": \"약속을 찾을 수 없어요\" }")
                    )),
            @ApiResponse(responseCode = "409", description = "데이터 충돌(향후 deterministic fingerprint 도입 시 사용)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 409, \"message\": \"약속키 데이터가 일치하지 않아요\" }")
                    )),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 401, \"message\": \"인증이 필요합니다.\" }")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{ \"code\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\" }")
                    ))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(value = "/promisekey2", produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<GetPromiseKey2> getPromiseKey2(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid GetPromiseRequest request) throws Exception{
        String userId = userPrincipal.getId();
        GetPromiseKey2 response = promiseManageInfoService.getPromiseKey2(userId, request);
        return new BaseResponse<>(response);
    }

}
