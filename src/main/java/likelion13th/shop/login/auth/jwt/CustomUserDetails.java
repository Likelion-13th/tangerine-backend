package likelion13th.shop.login.auth.jwt;

import likelion13th.shop.domain.Address;
import likelion13th.shop.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security에서 인증 주체(principal)로 사용하는 사용자 정보 객체
 * - 우리 서비스는 "providerId(카카오 고유 ID)"를 username으로 사용
 * - 비밀번호 기반 로그인이 아니므로 password는 사용하지 않음
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    // ───────────────────────────────── 기본 식별 정보 ─────────────────────────────────
    private Long userId;         // 내부 DB의 User PK
    private String providerId;   // 소셜 로그인 고유 식별자 (username으로 사용)
    private String usernickname; // 카카오 프로필 닉네임(표시용)
    private Address address;     // 예시: 프로필의 주소 정보

    // ──────────────────────────────── 권한(Authorization) ───────────────────────────────
    // - null일 수 있으므로 getAuthorities()에서 기본 ROLE_USER를 fallback으로 제공
    private Collection<? extends GrantedAuthority> authorities;

    // ──────────────────────────────── 생성자/팩토리 메서드 ───────────────────────────────

    // User 엔티티를 받아 기본 권한(ROLE_USER)을 가진 CustomUserDetails로 변환
    public CustomUserDetails(User user) {
        this.userId       = user.getId();
        this.providerId   = user.getProviderId();
        this.usernickname = user.getUsernickname();
        this.address      = user.getAddress();
        this.authorities  = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // (선택) username, password, 권한을 직접 받는 생성자
    // - 현재는 소셜 로그인이라 password를 사용하지 않으며, null/빈 문자열로 둠
    public CustomUserDetails(String providerId, String password, Collection<? extends GrantedAuthority> authorities) {
        this.providerId   = providerId;
        this.userId       = null;
        this.usernickname = null;
        this.authorities  = authorities;
        this.address      = null;
        // password는 무시됨 (소셜 로그인 시 사용하지 않음)
    }

    /**
     * User 엔티티 → CustomUserDetails 변환
     * - 권한(authorities)은 지정하지 않음 → getAuthorities()에서 ROLE_USER로 보완
     */
    public static CustomUserDetails fromEntity(User entity) {
        return CustomUserDetails.builder()
                .userId(entity.getId())                 // DB PK
                .providerId(entity.getProviderId())     // 소셜 고유 ID (username)
                .usernickname(entity.getUsernickname()) // 닉네임
                .address(entity.getAddress())           // 주소
                .build();
    }

    /**
     * CustomUserDetails → User 엔티티 변환
     * - 신규 저장 시 최소 필드만 세팅 (비밀번호 없음)
     * - 필요 시 등급/상태/타 필드를 서비스 레이어에서 채워 넣음
     */
    public User toEntity() {
        return User.builder()
                .id(this.userId)                         // PK (신규 생성이면 null 허용)
                .providerId(this.providerId)             // 소셜 고유 ID
                .usernickname(this.usernickname)         // 닉네임
                .address(this.address)                   // 주소
                .build();
    }

    // ──────────────────────────────── UserDetails 필수 구현 ───────────────────────────────

    @Override
    public String getUsername() {
        // username으로 providerId를 사용 (주요 인증 키)
        return this.providerId;
    }

    /**
     * 권한 조회
     * - 명시된 권한이 있으면 그대로 사용
     * - 없으면 기본 ROLE_USER 권한을 부여
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.authorities != null && !this.authorities.isEmpty()) {
            return this.authorities;
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        // 소셜 로그인은 비밀번호를 사용하지 않음
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료 정책 사용 시 실제 값으로 교체
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 잠금 정책 사용 시 실제 값으로 교체
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 자격 증명(비밀번호) 만료 정책 사용 시 실제 값으로 교체
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 활성/비활성 정책 사용 시 실제 값으로 교체 (예: 탈퇴/정지 사용자)
        return true;
    }
}

/*
1) 왜 필요한가
    - User와 Spring Security를 연결해주는 중요한 역할을 함

2) 없으면/틀리면?
    - 정보를 DB에서 일일히 조회하거나 해야하는 문제가 생김(번거로움)
    - 로그인이 안 될 수 있음
 */