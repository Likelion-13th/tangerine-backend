package likelion13th.shop.login.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 기반 인증 생성 필터 (provider_id 기반)
 * - 목적: /users/reissue 요청에서만, 만료되었을 수도 있는 AccessToken에서 provider_id(subject)를 꺼내
 *         "익명 권한(ROLE_ANONYMOUS)"으로 인증 컨텍스트를 만들어준다.
 * - 이유: 재발급 엔드포인트는 일반 인증과 다르게 '만료 토큰'으로도 본인 식별(provider_id)만 필요하기 때문.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCreationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    /**
     * 이 필터는 /users/reissue 경로에만 적용한다.
     * - shouldNotFilter가 true면 필터를 "건너뜀"
     * - 여기서는 해당 경로가 아니면 건너뛰도록 반대로 리턴한다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /users/reissue가 아니면 필터 적용 안 함
        return !"/users/reissue".equals(request.getServletPath());
    }

    /**
     * 재발급 전용 인증 생성 로직
     * - 이미 인증된 상태(익명 제외)면 그대로 통과
     * - Authorization: Bearer <token> 에서 토큰을 꺼내 subject(provider_id)만 확보
     * - 확보되면 ROLE_ANONYMOUS 권한으로 PreAuthenticatedAuthenticationToken을 주입
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1) 이미 인증이 존재하면 덮어쓰지 않고 그대로 진행 (익명 토큰은 제외)
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()
                && !(existing instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) Authorization 헤더 확인
        //    - 재발급 엔드포인트지만 헤더가 없거나 형식이 아니면 그냥 다음으로 넘긴다.
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3) "Bearer " 접두사 제거 후 JWT 본문만 추출
        String token = authHeader.substring("Bearer ".length());

        String providerId = null;
        try {
            // 4) 만료 허용 파싱: 재발급 시나리오에서는 만료된 AccessToken에서도 subject는 필요
            Claims claims = tokenProvider.parseClaimsAllowExpired(token);
            providerId = claims.getSubject(); // 우리 정책상 subject == provider_id
        } catch (Exception e) {
            // subject 추출 실패 시 인증을 만들지 않고 다음으로 넘긴다.
            filterChain.doFilter(request, response);
            return;
        }

        // 5) provider_id가 없으면 인증 컨텍스트를 만들지 않는다.
        if (providerId == null || providerId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 6) 재발급 과정 혼선을 피하기 위해 "익명 권한"만 부여
        var anonymousAuthorities = java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")
        );

        // 7) 이미 외부(토큰)에서 식별된 것으로 간주하는 PreAuthenticatedAuthenticationToken 사용
        //    - principal: providerId (문자열)
        //    - credentials: "N/A" (의미 없음)
        var preAuth = new org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken(
                providerId, "N/A", anonymousAuthorities
        );

        // 8) 새로운 SecurityContext를 만들어 주입
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(preAuth);
        SecurityContextHolder.setContext(context);

        // 9) 다음 필터 체인 진행
        filterChain.doFilter(request, response);
    }
}
