package likelion13th.shop.login.auth.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import likelion13th.shop.domain.Address;
import likelion13th.shop.domain.User;
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

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JpaUserDetailsManager jpaUserDetailsManager;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
        throws IOException {
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();

        String providerId = (String) oAuth2User.getAttributes().get("provider_Id");
        String nickname = (String) oAuth2User.getAttributes().get("nickname");

        if (!jpaUserDetailsManager.userExists(providerId)) {
            User newUser = User.builder()
                    .providerId(providerId)
                    .usernickname(nickname)
                    .deletable(true)
                    .build();

            newUser.setAddress(new Address("10540", "경기도 고양시 덕양구 항공대학로 76", "한국항공대학교"));

            CustomUserDetails userDetails = new CustomUserDetails(newUser);
            jpaUserDetailsManager.createUser(userDetails);
            log.info("신규 회원 등록");
        } else {
            log.info("기존 회원");
        }

        JwtDto jwt = userService.jwtMakeSave(providerId);

        String frontendRedirectUri = request.getParameter("redirect_uri");
        List<String> authorizeUris = List.of(
                "https://tangerine-likelion.netlify.app/",
                "http://localhost:3000"
        );
        if(frontendRedirectUri != null || authorizeUris.contains(frontendRedirectUri)) {
            frontendRedirectUri = "https://tanger-likelion.netlify.app/";
        }

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendRedirectUri)
                .queryParam("accessToken", jwt.getAccessToken())
                .build().toUriString();

        log.info("리다이렉트: {}", frontendRedirectUri);
        response.sendRedirect(redirectUrl);
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