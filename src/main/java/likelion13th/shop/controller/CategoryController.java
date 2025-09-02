package likelion13th.shop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import likelion13th.shop.DTO.response.ItemResponse;
import likelion13th.shop.global.api.ApiResponse;
import likelion13th.shop.global.api.SuccessCode;
import likelion13th.shop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Tag(name = "카테고리", description = "카테고리 관련 API 입니다.")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    // 상품 조회(카테고리별)
    // 컨트롤러에서 Optional 처리하고 있음
    // 컨트롤러에서는 예외처리만 하고자 함!
    /** 카테고리 별 상품 조회**/
    @GetMapping("/{categoryId}/items")
    @Operation(summary = "카테고리별 상품 조회", description = "상품을 카테고리 별로 조회합니다.")
    public ApiResponse<?> getItemsByCategory(@PathVariable Long categoryId) {
        List<ItemResponse> items = categoryService.getItemsByCategory(categoryId);

        //상품 없을 시 : 성공 응답 + 빈 리스트 반환
        if (items.isEmpty()) {
            return ApiResponse.onSuccess(SuccessCode.CATEGORY_ITEMS_EMPTY, Collections.emptyList());
        }

        return ApiResponse.onSuccess(SuccessCode.CATEGORY_ITEMS_GET_SUCCESS, items);

    }
}
