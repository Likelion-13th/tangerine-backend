package likelion13th.shop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import likelion13th.shop.DTO.request.OrderCreateRequest;
import likelion13th.shop.DTO.response.OrderResponse;
import likelion13th.shop.global.api.ApiResponse;
import likelion13th.shop.global.api.SuccessCode;
import likelion13th.shop.login.auth.jwt.CustomUserDetails;
import likelion13th.shop.login.service.UserService;
import likelion13th.shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;


@Tag(name = "주문", description = "주문 관련 API 입니다.")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /** 주문 생성 **/
    @PostMapping
    @Operation(summary = "주문 생성", description = "로그인한 사용자의 주문을 생성합니다.")
    public ApiResponse<?> createOrder(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody OrderCreateRequest request
    ) {
        OrderResponse newOrder = orderService.createOrder(request, customUserDetails);
        return ApiResponse.onSuccess(SuccessCode.ORDER_CREATE_SUCCESS, newOrder);
    }

    /** 모든 주문 목록 조회 **/
    @GetMapping
    @Operation(summary = "모든 주문 조회", description = "로그인한 사용자의 모든 주문을 목록으로 조회합니다.")
    public ApiResponse<?> getAllOrders(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        List<OrderResponse> orders = orderService.getAllOrders(customUserDetails);
        // 주문이 없더라도 성공 응답 + 빈 리스트 반환
        if (orders.isEmpty()) {
            return ApiResponse.onSuccess(SuccessCode.ORDER_LIST_EMPTY, Collections.emptyList());
        }
        return ApiResponse.onSuccess(SuccessCode.ORDER_LIST_SUCCESS, orders);
    }

    /** 주문 취소 **/
    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "로그인한 사용자의 주문을 취소합니다.")
    public ApiResponse<?> cancelOrder(@PathVariable Long orderId) {

        orderService.cancelOrder(orderId);

        return ApiResponse.onSuccess(SuccessCode.ORDER_CANCEL_SUCCESS, null);

    }
}
