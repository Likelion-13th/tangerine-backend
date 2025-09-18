package likelion13th.shop.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserRequestDto {
    @Schema(description = "UserReqDto")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserReqDto {
        // 카카오한테 받아올 때... 이름이랑 어쩌구 받는 통을 만들어줌
        private Long userId;
        private String providerId;
        private String usernickname;
    }

}
