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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "토큰", description = "Access Token 및 Refresh Token 관련 API")
@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class TokenController {
    private final UserService userService;

    @Operation(summary = "토큰 생성(회원가입 & 로그인)", description = "provider_id 기반으로 토큰 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER_200", description = "회원가입 & 로그인 성공")
    })
    @PostMapping("/generate")
    public ApiResponse<JwtDto> generateToken(@RequestBody UserReqDto userReqDto) {
        try {
            String providerId = userReqDto.getProviderId();

            JwtDto jwt = userService.jwtMakeSave(providerId);

            return ApiResponse.onSuccess(SuccessCode.USER_LOGIN_SUCCESS, jwt);
        } catch (GeneralException e){
            log.error("//회원가입, 로그인 중 에러 발견! {}", e.getReason().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("//예상 못한 에러도 나옴! {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}