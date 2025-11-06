package likelion13th.shop.global.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseCode { // 실패
    // Common
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러, 서버 개발자에게 문의하세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_400", "인증되지 않은 요청입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_4041", "존재하지 않는 회원입니다."),
    ALREADY_USED_NICKNAME(HttpStatus.FORBIDDEN, "USER_4031", "이미 사용중인 닉네임입니다."),
    OAUTH2_PROCESS_FAILED(HttpStatus.FORBIDDEN, "USER_2001", "OAuth2 사용자 정보 처리 실패"),

    USER_NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "AUTH_0001", "카카오 로그인 정보가 없습니다."),

    USER_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_4042", "사용자 정보를 찾을 수 없습니다."),
    USER_ORDERS_EMPTY(HttpStatus.NOT_FOUND, "USER_4043", "사용자의 주문 내역이 없습니다."),
    USER_ORDER_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_4044", "해당 주문이 없습니다."),

    INVALID_MILEAGE(HttpStatus.BAD_REQUEST, "USER_4045", "보유한 마일리지를 초과하여 사용 및 회수 할 수 없습니다."),

    // Jwt
    WRONG_REFRESH_TOKEN(HttpStatus.NOT_FOUND, "JWT_4041", "일치하는 리프레시 토큰이 없습니다."),
    IP_NOT_MATCHED(HttpStatus.FORBIDDEN, "JWT_4031", "리프레시 토큰의 IP주소가 일치하지 않습니다."),
    TOKEN_INVALID(HttpStatus.FORBIDDEN, "JWT_4032", "유효하지 않은 토큰입니다."),
    TOKEN_NO_AUTH(HttpStatus.FORBIDDEN, "JWT_4033", "권한 정보가 없는 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT_4011", "토큰 유효기간이 만료되었습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_4041", "해당 카테고리를 찾을 수 없습니다."),

    // Item
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_4041", "해당 상품을 찾을 수 없습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_4041", "해당 주문을 찾을 수 없습니다."),
    ORDER_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "ORDER_4001", "주문 취소에 실패했습니다."),
    ORDER_CREATE_FAILED(HttpStatus.BAD_REQUEST, "ORDER_4002", "주문 생성 요청이 잘못되었습니다."),

    // S3
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_500", "S3 업로드에 실패하였습니다."),
    S3_FILE_EMPTY(HttpStatus.BAD_REQUEST, "S3_400", "업로드할 파일이 비어 있습니다."),
    S3_INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "S3_401", "유효하지 않은 파일 형식입니다."),


    // User Address 관련 에러 코드 추가
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDRESS_4041", "해당 사용자의 주소 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    // 응답 코드 상세 정보 return
    @Override
    public ReasonDto getReason() {
        return ReasonDto.builder()
                .httpStatus(this.httpStatus)
                .code(this.code)
                .message(this.message)
                .build();
    }
}
