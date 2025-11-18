package likelion13th.shop.login.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
// 권장: org.springframework.transaction.annotation.Transactional 사용 (readOnly 등 추가 옵션 제공)
import jakarta.transaction.Transactional;

import likelion13th.shop.domain.User;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.exception.GeneralException;
import likelion13th.shop.login.auth.dto.JwtDto;
import likelion13th.shop.login.auth.jwt.RefreshToken;
import likelion13th.shop.login.auth.jwt.TokenProvider;
import likelion13th.shop.login.auth.repository.RefreshTokenRepository;
import likelion13th.shop.login.auth.service.JpaUserDetailsManager;
import likelion13th.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * UserService
 * - 사용자 조회/검증, JWT 생성/재발급, RefreshToken 저장/삭제를 담당하는 도메인 서비스
 * - 인증 키로 providerId(소셜 고유 ID)를 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final JpaUserDetailsManager manager;

    // ============================================================
    //  1) 회원 관련 서비스
    // ============================================================

    /** provider_id 존재 여부 빠른 확인 */
    public Boolean checkMemberByProviderId(String providerId) {
        return userRepository.existsByProviderId(providerId);
    }

    /** provider_id로 회원 찾기 (없을 수 있으므로 Optional) */
    public Optional<User> findByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId);
    }

    /** provider_id로 회원 강제 조회 (없으면 USER_NOT_FOUND 예외) */
    public User getAuthenticatedUser(String providerId) {
        return userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }

    // ============================================================
    //  2) Refresh Token 관련 서비스
    // ============================================================

    /**
     * Refresh Token 저장 또는 갱신
     * - providerId로 User를 조회한 뒤, 사용자당 1개의 RefreshToken 행을 upsert한다.
     */
    @Transactional
    public void saveRefreshToken(String providerId, String refreshToken) {
        // 1. 사용자 조회
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 2. 기존 토큰이 있으면 교체, 없으면 생성
        RefreshToken token = refreshTokenRepository.findByUser(user)
                .map(existingToken -> {
                    existingToken.updateRefreshToken(refreshToken);
                    return existingToken;
                })
                .orElseGet(() -> {
                    log.info("새 RefreshToken 생성 시도 (user_id={})", user.getId());
                    return RefreshToken.builder()
                            .user(user)                                   // User와 1:1
                            .refreshToken(refreshToken)                   // 실제 RT 문자열
                            .ttl(System.currentTimeMillis()
                                    + 1000L * 60 * 60 * 24)          // 만료 시각(예: 1일)
                            .build();
                });

        // 3. 저장
        refreshTokenRepository.save(token);
        log.info("RefreshToken 저장 완료 (user_id={})", user.getId());
    }

    // ============================================================
    //  3) JWT 생성 및 저장
    // ============================================================

    /**
     * Access/Refresh 토큰 동시 생성 + Refresh 저장
     * - providerId로 UserDetails를 로드 → TokenProvider로 JWT 생성 → RefreshToken upsert
     */
    @Transactional
    public JwtDto jwtMakeSave(String providerId) {
        log.info("UserDetailsManager 타입: {}", manager.getClass().getName());

        // 1. providerId 기반 사용자 로드 (Security용 UserDetails)
        UserDetails details = manager.loadUserByUsername(providerId);

        // 2. JWT 생성 (Access에 권한 포함, Refresh에는 권한 미포함)
        JwtDto jwt = tokenProvider.generateTokens(details);

        // 3. RefreshToken 저장/갱신
        saveRefreshToken(providerId, jwt.getRefreshToken());
        return jwt;
    }

    // ============================================================
    //  4) Refresh Token 기반 Access Token 재발급
    // ============================================================

    /**
     * Access 만료 상황에서의 재발급 엔드포인트용 로직
     * - 헤더 Authorization에서 Access 토큰을 받아 subject(providerId)를 추출한다.
     * - DB에 저장된 RefreshToken이 유효하면 새 Access/Refresh를 재발급한다(회전).
     */
    @Transactional
    public JwtDto reissue(HttpServletRequest request) {
        log.info("[STEP 1] Access Token 재발급 요청 시작");

        // 1. Authorization 헤더에서 Bearer 토큰 추출
        String accessToken = request.getHeader("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        // 2. (만료 허용) Claims 파싱으로 providerId(subject) 추출
        Claims claims;
        try {
            claims = tokenProvider.parseClaimsAllowExpired(accessToken);
        } catch (Exception e) {
            log.error("[ERROR] Access Token Claims 추출 실패: {}", e.getMessage());
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }
        String providerId = claims.getSubject();
        log.info("[STEP 2] Access Token에서 추출한 providerId: {}", providerId);

        if (providerId == null || providerId.isEmpty()) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        // 3. providerId로 사용자 조회
        User user = findByProviderId(providerId)
                .orElseThrow(() -> {
                    log.error("[ERROR] providerId={} 사용자 없음", providerId);
                    return new GeneralException(ErrorCode.USER_NOT_FOUND);
                });
        log.info("[STEP 3] User 조회 성공 (user_id={}, providerId={})", user.getId(), user.getProviderId());

        // 4. DB에 저장된 Refresh Token 조회
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_REFRESH_TOKEN));

        // 5. Refresh Token 유효성 검증 (서명/만료 등)
        if (!tokenProvider.validateToken(refreshTokenEntity.getRefreshToken())) {
            // 만료/위조 등으로 무효 → DB에서 삭제 후 만료 에러
            refreshTokenRepository.deleteByUser(user);
            log.error("[ERROR] Refresh Token 만료 또는 무효 - 삭제 완료 (user_id={})", user.getId());
            throw new GeneralException(ErrorCode.TOKEN_EXPIRED);
        }
        // (선택 강화) 주체 일치성 확인:
        // Claims rtClaims = tokenProvider.parseClaims(refreshTokenEntity.getRefreshToken());
        // if (!providerId.equals(rtClaims.getSubject())) { ... }

        // 6. 새 Access/Refresh 발급 (회전)
        UserDetails userDetails = manager.loadUserByUsername(providerId);
        JwtDto newJwt = tokenProvider.generateTokens(userDetails);
        log.info("[STEP 4] 새로운 Access/Refresh Token 발급 완료");

        // 7. Refresh 토큰 교체 저장
        refreshTokenEntity.updateRefreshToken(newJwt.getRefreshToken());
        refreshTokenRepository.save(refreshTokenEntity);

        return newJwt;
    }

    // ============================================================
    //  5) 로그아웃 서비스
    // ============================================================

    /**
     * 로그아웃
     * - 현재 Access Token의 subject(providerId)를 추출하여 해당 사용자의 RefreshToken 레코드를 삭제한다.
     */
    @Transactional
    public void logout(HttpServletRequest request) {
        // 1. Authorization 헤더에서 Access 토큰 추출
        String accessToken = request.getHeader("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        // 2. Access 토큰 파싱 (여기서는 만료 허용 없이 처리)
        Claims claims = tokenProvider.parseClaims(accessToken);
        String providerId = claims.getSubject();
        if (providerId == null || providerId.isEmpty()) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        // 3. providerId로 사용자 조회
        User user = findByProviderId(providerId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 4. Refresh Token 삭제 (세션 종료 효과)
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush(); // 즉시 반영이 꼭 필요하지 않다면 생략 가능
    }
}
