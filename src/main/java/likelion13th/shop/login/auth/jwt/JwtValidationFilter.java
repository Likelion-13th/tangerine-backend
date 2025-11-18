package likelion13th.shop.login.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import likelion13th.shop.global.api.ApiResponse;
import likelion13th.shop.global.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 유효성 검사 필터 (provider_id 기반)
 * - 모든 요청마다 1회 실행(OncePerRequestFilter)
 * - Authorization 헤더의 Bearer 토큰을 꺼내 유효성 검증
 * - 유효하면 SecurityContext에 인증(Authentication)을 주입
 * - 불필요한 중복 파싱/중복 인증 주입을 피하기 위해
 *   이미 인증된 요청은 바로 체인 진행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    /**
     * 특정 URL은 본 필터를 건너뛴다.
     * - /users/reissue : Refresh 토큰 재발급 엔드포인트
     *   (설계상 AuthCreationFilter에서 ROLE_ANONYMOUS를 주입해 처리)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/users/reissue".equals(request.getServletPath());
    }

    /**
     * 요청 처리 진입점
     * 1) 이미 인증된 요청이면 패스
     * 2) Authorization 헤더에서 Bearer 토큰 추출
     * 3) 토큰 파싱/검증 → providerId와 권한 복원
     * 4) SecurityContext에 Authentication 주입 후 체인 진행
     * 5) 예외 발생 시 표준 오류 응답(JSON) 반환
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        // [사전 차단] 이미 인증된 요청이면 추가 작업 없이 다음 필터로 진행
        //  - 익명 토큰(AnonymousAuthenticationToken)은 제외
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)) {
            chain.doFilter(request, response);
            return;
        }

        // Authorization 헤더 검사 (ex: "Bearer eyJhbGciOi...")
        //  - 헤더가 없거나 Bearer 스킴이 아니면 다음 필터로 넘김(비인증 상태 허용)
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // "Bearer " 이후의 실제 토큰 문자열 추출
        String token = authHeader.substring(7);

        try {
            // [핵심] 토큰을 한 번만 파싱하여 끝까지 사용 (중복 비용/오류 방지)
            Claims claims = tokenProvider.parseClaims(token);

            // sub(subject) == providerId (우리 정책)
            String providerId = claims.getSubject();
            if (providerId == null || providerId.isEmpty()) {
                sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
                return;
            }

            // 권한 복원 (없으면 ROLE_USER 기본 부여)
            var authorities = tokenProvider.getAuthFromClaims(claims);

            // Spring Security 표준 인증 토큰 구성
            CustomUserDetails userDetails = new CustomUserDetails(
                    providerId,
                    "",
                    authorities
            );
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

            // SecurityContext에 인증 주입 (요청 수명 동안 유효)
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // 민감 식별자 마스킹 로그 (운영 로그에 원문 노출 금지)
            String masked = providerId.length() > 4 ? providerId.substring(0, 4) + "***" : "***";
            log.debug("JWT 인증 성공 - subject(masked)={}", masked);

            // 다음 필터/컨트롤러로 진행
            chain.doFilter(request, response);

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // 서명 위조/구조 이상
            log.warn("잘못된 서명");
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
        } catch (ExpiredJwtException e) {
            // 만료(재로그인 또는 재발급 필요)
            log.warn("토큰 만료");
            sendErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 형식
            log.warn("지원되지 않는 토큰");
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
        } catch (IllegalArgumentException e) {
            // 널/공백 등 잘못된 입력
            log.warn("유효하지 않은 요청");
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
        } catch (Exception e) {
            // 그 외 예기치 못한 오류
            log.error("JWT 처리 중 알 수 없는 예외", e);
            sendErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 표준 에러 응답(JSON) 반환 유틸
     * - 상태 코드는 401(UNAUTHORIZED)로 통일
     *   (필요 시 INVALID=401, EXPIRED=401, 기타=500 등으로 세분화 가능)
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                new ObjectMapper().writeValueAsString(ApiResponse.onFailure(errorCode, null))
        );
    }
}

/*
 * [리뷰 & 운영 팁]
 * 1) ObjectMapper 매번 생성 대신, 상수/빈으로 재사용하면 GC 부하를 줄일 수 있습니다.
 * 2) 필요 시 403(FORBIDDEN)과 401(UNAUTHORIZED)을 구분해 응답하도록 확장 가능합니다.
 * 3) Clock skew가 크다면 TokenProvider 파서에 허용 오차를 설정하세요.
 * 4) 필터 순서: 이 필터가 UsernamePasswordAuthenticationFilter 이전에 오도록 SecurityConfig에서 순서를 확인하세요.
 */
