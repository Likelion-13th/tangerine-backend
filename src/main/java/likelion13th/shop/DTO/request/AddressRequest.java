package likelion13th.shop.DTO.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddressRequest {
    private String zipcode;       // 사용자가 변경 가능
    private String address;       // 사용자가 변경 가능
    private String addressDetail; // 사용자가 변경 가능
}