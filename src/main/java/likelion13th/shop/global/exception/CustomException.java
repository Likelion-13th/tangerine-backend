package likelion13th.shop.global.exception;

import likelion13th.shop.global.api.BaseCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final BaseCode errorCode;

    public CustomException(BaseCode errorCode) {
        super(errorCode.getReason().getMessage());
        this.errorCode = errorCode;
    }
}