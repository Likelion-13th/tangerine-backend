package likelion13th.shop.repository;

import likelion13th.shop.domain.Order;
import likelion13th.shop.global.constant.OrderStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // OrderRepository.java의 패턴을 그대로 따라하세요!
    // JPA 쿼리 메서드 활용

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);
}