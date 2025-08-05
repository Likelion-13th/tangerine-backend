package likelion13th.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Embeddable
@AllArgsConstructor
@Getter
public class Address {

    @Column(nullable = false)
    private String zipcode;

    @Column(nullable = false)
    private String address; //

    @Column(nullable = false)
    private String addressDetail; //

    public Address() {
        this.zipcode = "10540";
        this.address = "경기도 고양시 덕양구 항공대학로 76";
        this.addressDetail = "한국항공대학교";
    }
}