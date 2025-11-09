package likelion13th.shop.login.auth.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import likelion13th.shop.domain.Address;
import likelion13th.shop.domain.User;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.exception.GeneralException;
import likelion13th.shop.login.auth.dto.JwtDto;
import likelion13th.shop.login.auth.jwt.CustomUserDetails;
import likelion13th.shop.login.auth.service.JpaUserDetailsManager;
import likelion13th.shop.login.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JpaUserDetailsManager jpaUserDetailsManager; // Security 사용자 저장/조회 담당
    private final UserService userService;                     // JWT 발급 및 RefreshToken 저장 로직

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "https://tangerine-likelion.netlify.app/",
            "http://localhost:3000"
    );
    private static final String DEFAULT_FRONT_ORIGIN = "https://tangerine-likelion.netlify.app/";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            // 1️⃣ providerId, nickname 추출
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
            String providerId = String.valueOf(oAuth2User.getAttributes().getOrDefault("provider_id", oAuth2User.getAttributes().get("id")));
            String nickname = (String) oAuth2User.getAttributes().get("nickname");
            log.info("// [OAuth2Success] providerId={}, nickname={}", providerId, nickname);

            // 2️⃣ 신규 회원 등록 여부 확인 후 없으면 생성
            if (!jpaUserDetailsManager.userExists(providerId)) {
                User newUser = User.builder()
                        .providerId(providerId)
                        .usernickname(nickname)
                        .deletable(true)
                        .build();

                // 예시 주소 설정 (테스트용)
                newUser.setAddress(new Address("10540", "경기도 고양시 덕양구 항공대학로 76", "한국항공대학교"));
                log.info("// 신규 회원 address 확인: {}", newUser.getAddress().getAddress());

                CustomUserDetails userDetails = new CustomUserDetails(newUser);
                jpaUserDetailsManager.createUser(userDetails);
                log.info("// 신규 회원 등록 완료 (provider_id={})", providerId);
            } else {
                log.info("// 기존 회원 로그인 (provider_id={})", providerId);
            }

            // 3️⃣ JWT 발급 및 RefreshToken 저장
            JwtDto jwt = userService.jwtMakeSave(providerId);
            log.info("// JWT 발급 완료 (provider_id={})", providerId);

            // 4️⃣ 세션에서 redirect origin 회수 후 제거
            String frontendRedirectOrigin = (String) request.getSession().getAttribute("FRONT_REDIRECT_URI");
            request.getSession().removeAttribute("FRONT_REDIRECT_URI");

            // 5️⃣ 화이트리스트 재검증
            if (frontendRedirectOrigin == null || !ALLOWED_ORIGINS.contains(frontendRedirectOrigin)) {
                frontendRedirectOrigin = DEFAULT_FRONT_ORIGIN;
            }

            // 6️⃣ 최종 리다이렉트 URL 생성 (accessToken 포함)
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendRedirectOrigin)
                    .queryParam("accessToken", URLEncoder.encode(jwt.getAccessToken(), StandardCharsets.UTF_8))
                    .build(true)
                    .toUriString();

            log.info("// [OAuth2Success] Redirecting to {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (GeneralException e) {
            log.error("// [OAuth2Success] GeneralException: {}", e.getReason().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("// [OAuth2Success] Unexpected Error: {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

/*
1) 왜 필요한가
    - 소셜 로그인이 성공하고 진행을 하는데 필요함
    - access token을 전달하는데 필요함

2) 없으면/틀리면?
    - 소셜 로그인이 소용 없어짐
    - 보안에 매우 큰 구멍이 생김...
 */

