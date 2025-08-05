package likelion13th.shop.global.exception;

import likelion13th.shop.global.api.BaseCode;
import likelion13th.shop.global.api.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 공통 예외 처리
@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private final BaseCode code;

    //예외 생성
    public static GeneralException of(BaseCode code) {
        return new GeneralException(code);
    }

    //예외 상세 정보
    public ReasonDto getReason() {
        return this.code.getReason();
    }
}
