package likelion13th.shop.login.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import likelion13th.shop.global.api.ApiResponse;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.api.SuccessCode;
import likelion13th.shop.global.exception.GeneralException;
import likelion13th.shop.login.auth.dto.JwtDto;
import likelion13th.shop.login.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원", description = "회원 관련 API (토큰 재발급, 로그아웃) 입니다.")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
public class UserController {

    // 서비스 계층: 재발급/로그아웃 로직을 모두 위임
    private final UserService userService;

    // ───────────────────────────────────────────────────────────────────
    //  토큰 재발급 API
    //  - Access 토큰이 만료된 경우, DB에 저장된 Refresh 토큰을 검증하여
    //    새로운 Access/Refresh 토큰을 발급한다.
    //  - Access 토큰은 요청 헤더 Authorization에서 읽고, providerId(subject)를 추출한다.
    // ───────────────────────────────────────────────────────────────────
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용해 새로운 Access Token을 발급하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @PostMapping("/reissue")
    public ApiResponse<JwtDto> reissue(HttpServletRequest request) {
        log.info("[STEP 1] 토큰 재발급 요청 수신");

        try {
            // 서비스에 위임: 헤더에서 Access 토큰 파싱 → Refresh 검증 → 새 JWT 발급
            JwtDto jwt = userService.reissue(request);
            log.info("[STEP 2] 토큰 재발급 성공 - 새로운 Access/Refresh 반환");
            return ApiResponse.onSuccess(SuccessCode.USER_REISSUE_SUCCESS, jwt);

        } catch (GeneralException e) {
            // 표준 에러코드 기반 예외는 그대로 전파 (ControllerAdvice에서 공통 처리 가능)
            log.error("[ERROR] 토큰 재발급 중 예외: {}", e.getReason().getMessage());
            throw e;

        } catch (Exception e) {
            // 알 수 없는 예외는 내부 서버 에러로 래핑
            log.error("[ERROR] 예상치 못한 예외: {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ───────────────────────────────────────────────────────────────────
    //  로그아웃 API
    //  - 현재 사용자의 Access 토큰에서 providerId(subject)를 읽어
    //    해당 사용자의 Refresh 토큰 레코드를 DB에서 삭제한다(세션 종료 효과).
    //  - Access 토큰 파싱은 서비스에서 수행한다.
    // ───────────────────────────────────────────────────────────────────
    @Operation(summary = "로그아웃", description = "사용자를 로그아웃 처리하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @DeleteMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        // 서비스에 위임: 헤더에서 Access 토큰 → subject 추출 → Refresh 삭제
        userService.logout(request);
        return ApiResponse.onSuccess(SuccessCode.USER_LOGOUT_SUCCESS, null);
    }
}
