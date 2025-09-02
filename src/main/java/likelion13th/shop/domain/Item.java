package likelion13th.shop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import likelion13th.shop.domain.entity.BaseEntity;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "item")
@NoArgsConstructor
@AllArgsConstructor
public class Item extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String imagePath;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private boolean isNew= false;

    //Category와 다대다 연관관계 설정
    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<>();


    /** Order과 일대다 연관관계 설정
     * -> Item에서 Order의 목록을 볼 일이 없으므로 단방향 처리 **/
}
