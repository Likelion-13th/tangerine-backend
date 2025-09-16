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

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {
    private Long userId;
    private String providerId;
    private String usernickname;
    private Address address;

    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.providerId = user.getProviderId();
        this.usernickname = user.getUsernickname();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public CustomUserDetails(String providerId, String password, Collection<? extends GrantedAuthority> authorities) {
        this.providerId = providerId;
        this.userId = null;
        this.usernickname = null;
        this.authorities = authorities;
        this.address = null;
    }

    // User 엔티티 -> CustomUserDetails 변환
    public static CustomUserDetails fromEntity(User entity) {
        return CustomUserDetails.builder()
                .userId(entity.getId())
                .providerId(entity.getProviderId())
                .usernickname(entity.getUsernickname())
                .address(entity.getAddress())
                .build();
    }

    // User 엔티티 <- CustomUserDetails 변환
    public User toEntity() {
        return User.builder()
                .id(this.userId)
                .providerId(this.providerId)
                .usernickname(this.usernickname)
                .address(this.address)
                .build();
    }

    @Override
    public String getUsername(){
        return this.providerId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(this.authorities != null && this.authorities.isEmpty()){
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