package likelion13th.shop.DTO.request;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCreateRequest {
    private Long itemId;
    private int quantity;
    private int mileageToUse;
}