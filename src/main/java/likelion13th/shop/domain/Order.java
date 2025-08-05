package likelion13th.shop.domain;

import jakarta.persistence.*;
import likelion13th.shop.domain.entity.BaseEntity;
import likelion13th.shop.global.constant.OrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(name = "orders") //예약어 회피
@NoArgsConstructor
//파라미터가 없는 디폴트 생성자 자동으로 생성
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    @Setter
    private int totalPrice; //기존 주문 내역을 유지하기 위해

    @Column(nullable = false)
    @Setter
    private int finalPrice;

    @Setter
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    //Item, User 와 연관관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    //생성자 -> 객체 생성될 때 자동으로 실행! 즉 초기 설정을 할 때 사용
    public Order(User user, Item item, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
        }

        this.user = user;
        this.item = item;
        this.quantity = quantity;
        this.status = OrderStatus.PROCESSING;
        this.totalPrice = item.getPrice() * quantity;

        // 연관관계 편의 메서드 호출
        user.getOrders().add(this);
        item.getOrders().add(this);
    }

    // 주문 상태 업데이트
    public void updateStatus(OrderStatus status) {
        this.status = status;
    }


    //양방향 편의 메서드
    public void setUser(User user) {
        this.user = user;
        if (!user.getOrders().contains(this)) {
            user.getOrders().add(this);
        } // 반대쪽 객체에도 연관관계를 설정
    }

    public void setItem(Item item) {
        this.item = item;
        if (!item.getOrders().contains(this)) {
            item.getOrders().add(this);
        }
    }
}

// 주문 생성 시 필수 데이터 초기화 및 연관관계 설정 담당
// 상태 변경 및 양방향 연관관계 처리를 위한 비즈니스 로직 포함