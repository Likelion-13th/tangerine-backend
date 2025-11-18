package likelion13th.shop.DTO.response;


import likelion13th.shop.domain.Order;
import likelion13th.shop.global.constant.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String usernickname;
    private int quantity;
    private int totalPrice;
    private int finalPrice;
    private int mileageToUse;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private ItemResponse item;

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getUsernickname(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getFinalPrice(),
                order.getTotalPrice() - order.getFinalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                ItemResponse.from(order.getItem())
        );
    }
}
