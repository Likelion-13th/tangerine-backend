package likelion13th.shop.service;

import jakarta.transaction.Transactional;
import likelion13th.shop.DTO.request.OrderCreateRequest;
import likelion13th.shop.DTO.response.OrderResponseDto;
import likelion13th.shop.domain.Item;
import likelion13th.shop.domain.Order;
import likelion13th.shop.domain.User;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.constant.OrderStatus;
import likelion13th.shop.global.exception.GeneralException;
import likelion13th.shop.repository.ItemRepository;
import likelion13th.shop.repository.OrderRepository;
import likelion13th.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    //마일리지 적용 후 가격에 대한 로직
    private int calculateFinalPrice(int totalPrice, int mileageToUse) {
        // 사용 가능한 최대 마일리지
        int availableMileage = Math.min(mileageToUse, totalPrice);
        // 최종 결제 금액
        int finalPrice = totalPrice - availableMileage;
        return Math.max(finalPrice, 0);  // 최소 결제 금액 0원 보장
    }

    // 주문 생성
    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequest request, User user) {
        // 상품 조회
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new GeneralException(ErrorCode.ITEM_NOT_FOUND));

        // 총 주문 금액 계산
        int totalPrice = item.getPrice() * request.getQuantity();
        // 마일리지 유효성 검사
        int mileageToUse = request.getMileageToUse();
        if (mileageToUse > user.getMaxMileage()) {
            throw new GeneralException(ErrorCode.INVALID_MILEAGE);
            //throw new IllegalArgumentException("보유한 마일리지를 초과하여 사용할 수 없습니다.");
        }

        // 최종 금액 계선
        int finalPrice = calculateFinalPrice(totalPrice, mileageToUse); // 최종 결제 금액

        //주문 생성과 동시에 배송 중으로 설정
        Order order = new Order(user, item, request.getQuantity());
        order.setTotalPrice(totalPrice);
        order.setFinalPrice(finalPrice);
        order.setStatus(OrderStatus.PROCESSING);
        //사용자 마일리지 처리
        user.useMileage(mileageToUse);
        user.addMileage((int) (finalPrice * 0.1));//결제 금액의 10% 마일리지 적립
        //최근 결제 금액 업데이트
        user.updateRecentTotal(finalPrice);
        //주문 저장
        orderRepository.save(order);

        return OrderResponseDto.from(order);
    }

    //개별 주문 조회
    @Transactional
    public OrderResponseDto getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponseDto::from)
                .orElseThrow(()->new GeneralException(ErrorCode.ORDER_NOT_FOUND));
    }

    //사용자의 모든 주문 조회
    @Transactional
    public List<OrderResponseDto> getAllOrders(User user) {
        //프록시 객체 -> DTO로 변환 후 반환
        return user.getOrders().stream()
                .map(OrderResponseDto::from)
                .collect(Collectors.toList());

    }

    //삭제가 아니라 주문 상태만 변경
    //배송 완료된 상품, 주문 취소된 상품은 주문 취소 불가능
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new GeneralException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.COMPLETE || order.getStatus() == OrderStatus.CANCEL) {
            throw new GeneralException(ErrorCode.ORDER_CANCEL_FAILED);
        }


        User user = order.getUser();
        // 회수해야할 마일리지보다 가지고 있는 마일리지가 적을 경우
        if (user.getMaxMileage() < (int) (order.getFinalPrice() * 0.1)) {
            throw new GeneralException(ErrorCode.INVALID_MILEAGE);
        }
        //주문 상태 변경
        order.setStatus(OrderStatus.CANCEL);
        // 결제 시에 적립되었던 마일리지 차감 ( 결제 금액의 10%)
        user.useMileage((int) (order.getFinalPrice() * 0.1));

        //마일리지 환불
        user.addMileage(order.getTotalPrice() - order.getFinalPrice());

        // 주문 취소 시, 해당 주문의 총 결제 금액 차감
        user.updateRecentTotal(-order.getTotalPrice());


        //return OrderResponseDto.from(order);
        //return true;
    }


    @Scheduled(fixedRate = 3600000) // 이럿케 수정해달랫음
    @Transactional
    public void updateOrderStatus() {
        // PROCESSING 상태면서 1 시간 이전에 생성된 주문 찾는 메서드
        List<Order> orders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PROCESSING,
                LocalDateTime.now().minusMinutes(1)
        );

        // 주문 상태를 'COMPLETE' 로 변경
        for (Order order : orders) {
            order.setStatus(OrderStatus.COMPLETE);
        }
    }

}
