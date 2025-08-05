package likelion13th.shop.DTO.response;


import likelion13th.shop.domain.Order;
import likelion13th.shop.global.constant.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private String usernickname;
    private String item_name;
    private int quantity;
    private int totalPrice;
    private int finalPrice;
    private int mileageToUse; //사용한 마일리지
    private OrderStatus status;
    private LocalDateTime createdAt;

    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getUser().getUsernickname(),
                order.getItem().getItemName(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getFinalPrice(),
                order.getTotalPrice() - order.getFinalPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
