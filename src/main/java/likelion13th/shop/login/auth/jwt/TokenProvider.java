package likelion13th.shop.login.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.exception.GeneralException;
import likelion13th.shop.login.auth.dto.JwtDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * TokenProvider
 * - JWT(Access/Refresh) 생성·검증·파싱을 담당
 *
 * 토큰 설계:
 * - subject(sub): providerId(카카오 고유 ID)를 저장
 * - iat(발급시각), exp(만료시각) 기본 포함
 * - authorities(문자열): Access Token에만 포함, Refresh Token에는 포함하지 않음
 *
 * 서명 알고리즘:
 * - HS256 (대칭키) 사용
 * - secretKey는 32바이트(256비트) 이상이 권장됨 (jjwt Keys.hmacShaKeyFor 요구사항)
 *   예: 환경변수에 충분히 긴 랜덤 바이트를 Base64로 넣어 사용
 */
@Slf4j
@Component
public class TokenProvider {

    private final Key secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    /**
     * 생성자: 설정(application.yml / 환경변수)에서 키와 만료 시간 주입
     *
     * @param secretKey              HS256 서명용 시크릿 (32바이트 이상 권장)
     * @param accessTokenExpiration  Access Token 만료(ms)
     * @param refreshTokenExpiration Refresh Token 만료(ms)
     *
     * 예시(yml):
     *   JWT_SECRET: ${JWT_SECRET}
     *   JWT_EXPIRATION: 900000        # 15분
     *   JWT_REFRESH_EXPIRATION: 1209600000 # 14일
     */
    public TokenProvider(
            @Value("${JWT_SECRET}") String secretKey,
            @Value("${JWT_EXPIRATION}") long accessTokenExpiration,
            @Value("${JWT_REFRESH_EXPIRATION}") long refreshTokenExpiration) {
        // jjwt가 내부적으로 키 길이를 검사하므로, 충분히 긴 바이트 배열이어야 함
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * AccessToken 및 RefreshToken 동시 생성
     * - UserDetails에서 providerId(username)와 권한을 읽어 AccessToken에만 권한을 claim으로 넣는다.
     */
    public JwtDto generateTokens(UserDetails userDetails) {
        log.info("JWT 생성 시작: 사용자 {}", userDetails.getUsername());

        // username == providerId (우리 서비스 정책)
        String userId = userDetails.getUsername();

        // 권한 목록을 "ROLE_USER,ROLE_ADMIN" 형태의 문자열로 직렬화 (AccessToken에만 저장)
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // Access Token 생성 (권한 포함)
        String accessToken = createToken(userId, authorities, accessTokenExpiration);

        // Refresh Token 생성 (권한 불포함: 재발급 용도로만 사용)
        String refreshToken = createToken(userId, null, refreshTokenExpiration);

        log.info("Access/Refresh 토큰 생성 완료 (userId: {})", userId);
        return new JwtDto(accessToken, refreshToken);
    }

    /**
     * 공통 JWT 생성 로직
     *
     * @param providerId     사용자 식별자(= provider_id) → JWT의 subject로 저장
     * @param authorities    권한 문자열 (Access 전용, 예: "ROLE_USER,ROLE_ADMIN")
     * @param expirationTime 만료 시간(ms)
     */
    private String createToken(String providerId, String authorities, long expirationTime) {
        JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(providerId)                                   // sub
                .setIssuedAt(new Date())                                  // iat
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // exp
                .signWith(secretKey, SignatureAlgorithm.HS256);           // 서명

        // Access Token에만 권한을 실어 보낸다.
        if (authorities != null) {
            jwtBuilder.claim("authorities", authorities);
        }

        return jwtBuilder.compact();
    }

    /**
     * 토큰 유효성 검증
     * - 서명/구조/만료 등을 검증하고, 예외가 없으면 유효한 것으로 간주
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token); // 파싱 성공 == 유효
            return true;
        } catch (JwtException e) {
            // SignatureException, MalformedJwtException, ExpiredJwtException 등 모두 포함
            log.warn("JWT 검증 실패: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 토큰에서 Claims 추출
     * - 만료(ExpiredJwtException) 시 예외를 상위로 던져 호출부에서 별도 처리
     * - 그 외 파싱 실패는 TOKEN_INVALID로 래핑
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("토큰 만료: {}", e.getClass().getSimpleName());
            throw e; // 재발급 플로우 등 호출부에서 만료를 구분 처리
        } catch (JwtException e) {
            log.warn("JWT 파싱 실패: {}", e.getClass().getSimpleName());
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * Claims → 권한 정보 복원
     * - "ROLE_USER,ROLE_ADMIN" 문자열을 SimpleGrantedAuthority 리스트로 변환
     * - 권한이 없으면 기본 ROLE_USER 부여 (게스트 최소 권한)
     */
    public Collection<? extends GrantedAuthority> getAuthFromClaims(Claims claims) {
        String authoritiesString = claims.get("authorities", String.class);
        if (authoritiesString == null || authoritiesString.isEmpty()) {
            log.warn("권한 정보 없음 - 기본 ROLE_USER 부여");
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return Arrays.stream(authoritiesString.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * 만료 허용 파싱
     * - 만료된 토큰이라도 Claims만 뽑아 써야 하는 경우(예: Refresh로 재발급) 사용
     */
    public Claims parseClaimsAllowExpired(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료됐지만 payload(Claims)는 안전하게 획득 가능
            return e.getClaims();
        }
    }
}

/*
 * [실무 팁]
 * 1) 토큰/시크릿키를 절대 로그로 직접 출력하지 않기 (특히 Production)
 * 2) 전송은 반드시 HTTPS로만 (중간자 공격 방지)
 * 3) 키 순환(Key Rotation)을 고려: kid 헤더 도입 → 키 변경 시점에 대비
 * 4) Clock Skew(서버-클라이언트 시계 차) 허용이 필요하면 parserBuilder().setAllowedClockSkewSeconds(...) 활용
 * 5) Refresh Token 저장 전략:
 *    - DB/Redis 등에 블랙리스트/화이트리스트로 관리하거나, 최신 토큰만 유효하도록 설계
 * 6) 쿠키로 전달할 때는 HttpOnly + Secure + SameSite 옵션을 반드시 설정
 */

/*
1) 왜 필요한가
    - 사용자별 refresh token을 안전하게 보관하기 위해 필요함

2) 없으면/틀리면?
    - 토큰 무효화/재발급이 불가능함
    - 보안에 매우 큰 구멍이 생김
 */