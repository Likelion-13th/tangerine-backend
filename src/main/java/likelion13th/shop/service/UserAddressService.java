package likelion13th.shop.service;

import likelion13th.shop.domain.User;
import likelion13th.shop.domain.Address;
import likelion13th.shop.DTO.request.AddressRequest;
import likelion13th.shop.DTO.response.AddressResponse;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.exception.CustomException;
import likelion13th.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserRepository userRepository;

    // 사용자 주소 저장 (기본값 또는 변경)
    @Transactional
    public AddressResponse saveAddress(String providerId, AddressRequest request) {
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 입력한 값이 없을 경우 기본 주소 사용
        String zipcode = request.getZipcode();
        String address = request.getAddress();
        String detail = request.getAddressDetail();

        // 새로운 주소 설정
        Address newAddress = new Address(zipcode, address, detail);
        user.updateAddress(newAddress); // User 엔티티에 주소 업데이트
        userRepository.save(user); // 변경 사항 저장

        return new AddressResponse(user.getAddress());
    }

    // 사용자 주소 조회 (기본값 -> 항공대로 제공)
    @Transactional(readOnly = true)
    public AddressResponse getAddress(String providerId) {
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new AddressResponse(user.getAddress());
    }
}