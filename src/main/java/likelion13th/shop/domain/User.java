package likelion13th.shop.domain;

import jakarta.persistence.*;
import likelion13th.shop.domain.entity.BaseEntity;
import likelion13th.shop.login.auth.jwt.RefreshToken;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder // 로그인 관련
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    // 카카오 고유 ID
    @Column(nullable = false, unique = true)
    private String providerId;

    // 카카오 닉네임 (중복 허용)
    @Column(nullable = false)
    private String usernickname;

    // 휴대폰 번호 (선택 사항, 기본값 null)
    @Column(nullable = true)
    private String phoneNumber;

    // 계정 삭제 가능 여부 (기본값 true)
    @Column(nullable = false)
    // @Setter
    private boolean deletable = true;

    // 마일리지 (기본값 0, 비즈니스 메서드로만 관리)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    /*테이블 단위로 세터가 적용되어있을 경우 얘만 제외시키거나
    의도적으로 세터 안넣은거라고 명시적이게 표기 */
    private int maxMileage = 0;

    // 최근 총 구매액 (기본값 0, 비즈니스 메서드로만 관리)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private int recentTotal = 0;

    // Refresh Token 관계 설정 (1:1)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken auth;

    // 주소 정보 (임베디드 타입)
    @Setter
    @Embedded
    private Address address;

    // 주문 정보 (1:N 관계)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // 주문 추가 메서드
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }

    /**
     * 도메인 내에서 처리 가능한 비즈니스 로직 또는 세터 대체 메서드
     * 도메인 보호를 위해 유효성 검사도 해줍니당.
     **/

    // 마일리지 사용
    public void useMileage(int mileage) {
        if (mileage < 0) {
            throw new IllegalArgumentException("사용할 마일리지는 0보다 커야 합니다.");
        }
        if (this.maxMileage < mileage) {
            throw new IllegalArgumentException("마일리지가 부족합니다.");
        }
        this.maxMileage -= mileage;
    }

    // 마일리지 적립
    public void addMileage(int mileage) {
        if (mileage < 0) {
            throw new IllegalArgumentException("적립할 마일리지는 0보다 커야 합니다.");
        }
        this.maxMileage += mileage;
    }

    // 총 결제 금액 업데이트
    public void updateRecentTotal(int amount) {
        // 취소의 경우도 있어서 amount에 대한 유효성 검사는 따로 x
        int newTotal = this.recentTotal + amount;
        if (newTotal < 0) {
            throw new IllegalArgumentException("총 결제 금액은 음수가 될 수 없습니다.");
        }
        this.recentTotal = newTotal;
    }

    // 주소 저장/수정 메서드 추가
    public void updateAddress(Address address) {
        this.address = address;
    }
}
