package likelion13th.shop.login.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@ToString
@Getter
@Setter
public class JwtDto {
    private String accessToken;
    private String refreshToken;

    public JwtDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
/*
1) 왜 필요한가?
    - 토큰을 전달하기 위해 꼭 필요함
    - 두 개의 토큰을 따로 전달하면 번거롭지만, 한 번에 해 버리면 수정할 때에도 편함
2) 없으면/틀리면?
    - 보통 jwtdto를 이용해서 코드를 짜기 때문에,
      jwtdto가 없다면 협업할 때 서로 헷갈림
    - 인증에 어려움이 생김
 */