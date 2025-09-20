package likelion13th.shop.login.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import likelion13th.shop.global.api.ApiResponse;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.api.SuccessCode;
import likelion13th.shop.global.exception.GeneralException;
import likelion13th.shop.login.auth.dto.JwtDto;
import likelion13th.shop.login.dto.UserRequestDto.UserReqDto;
import likelion13th.shop.login.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 토큰 관련 컨트롤러
 * - /token/generate : provider_id를 받아 Access/Refresh 토큰 발급
 * - /token/local    : 로컬 환경에서 토큰 문자열을 그대로 확인(테스트용)
 *
 * 주의:
 * - 운영 환경에서는 Refresh Token을 응답 본문으로 직접 반환하지 않고,
 *   HttpOnly + Secure 쿠키로 내려보내는 방식을 권장합니다.
 */
@Slf4j
@Tag(name = "토큰", description = "Access Token 및 Refresh Token 관련 API")
@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class TokenController {

    private final UserService userService;

    // ==========================================
    //  1) 토큰 생성 (회원가입 & 로그인)
    // ==========================================

    /**
     * 토큰 생성 (회원가입 & 로그인)
     * - 입력: provider_id (소셜 고유 식별자)
     * - 처리: provider_id로 UserDetails 로드 → Access/Refresh 생성 및 Refresh 저장
     * - 출력: JwtDto(accessToken, refreshToken)
     */
    @Operation(
            summary = "토큰 생성 (회원가입 & 로그인)",
            description = "provider_id 기반으로 JWT Access/Refresh 토큰을 반환합니다."
    )
    @ApiResponses({
            // 스웨거 응답 코드는팀 표준(SuccessCode) 설명용이며, 실제 HTTP 코드는 200이 내려갑니다.
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER_2011", description = "회원가입 & 로그인 성공")
    })
    @PostMapping("/generate")
    public ApiResponse<JwtDto> generateToken(@RequestBody UserReqDto userReqDto) {
        try {
            String providerId = userReqDto.getProviderId(); // // DTO에서 provider_id 추출
            log.info("// [generateToken] 요청받은 providerId: {}", providerId);

            // // Access/Refresh 동시 발급 + Refresh 저장
            JwtDto jwt = userService.jwtMakeSave(providerId);
            log.info("// 토큰 발급 성공 (providerId: {})", providerId);

            // // 표준 응답 래핑
            return ApiResponse.onSuccess(SuccessCode.USER_LOGIN_SUCCESS, jwt);

        } catch (GeneralException e) {
            // // 도메인 표준 예외(코드/메시지 보유)는 그대로 전파
            log.error("// 회원가입/로그인 중 에러 발생: {}", e.getReason().getMessage());
            throw e;
        } catch (Exception e) {
            // // 예기치 못한 오류는 내부 서버 에러로 래핑
            log.error("// 예상치 못한 에러 발생: {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================
    //  2) 로컬 테스트용 토큰 반환 API
    // ==========================================

    /**
     * 로컬 테스트용 토큰 반환
     * - 쿼리 파라미터로 받은 access-token, refresh-token 값을 그대로 되돌려줌
     * - 개발/테스트 환경에서만 사용 권장 (운영 배포 시 비활성화 권장)
     */
    @Operation(
            summary = "로컬 테스트용 토큰 반환",
            description = "Access Token과 Refresh Token을 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON_200", description = "토큰 반환 성공")
    })
    @GetMapping("/local")
    public ApiResponse<Map<String, String>> getTokenInfo(
            @RequestParam(name = "access-token") String accessToken,
            @RequestParam(name = "refresh-token") String refreshToken
    ) {
        // // 단순 에코 응답 (테스트 확인용)
        Map<String, String> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);
        responseData.put("refreshToken", refreshToken);

        log.info("// 로컬 테스트용 토큰 반환 완료");
        return ApiResponse.onSuccess(SuccessCode.OK, responseData);
    }
}
