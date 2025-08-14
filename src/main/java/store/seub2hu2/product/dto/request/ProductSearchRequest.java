package store.seub2hu2.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 상품 검색 요청 DTO
 */
@Getter
@Setter
@ToString
public class ProductSearchRequest {

    @NotNull(message = "상위 카테고리는 필수입니다")
    @Min(value = 1, message = "상위 카테고리는 1 이상이어야 합니다")
    private Integer topNo;

    private Integer catNo = 0;

    @Min(value = 1, message = "페이지는 1 이상이어야 합니다")
    private Integer page = 1;

    @Min(value = 1, message = "행 수는 1 이상이어야 합니다")
    private Integer rows = 6;

    private String sort = "date";

    private String opt;   // 검색 옵션: name, minPrice, maxPrice
    private String value; // 검색 값

    /**
     * 카테고리 필터가 적용되었는지 확인
     */
    public boolean hasCategoryFilter() {
        return catNo != null && catNo > 0;
    }

    /**
     * 검색 조건이 있는지 확인
     */
    public boolean hasSearchCondition() {
        return opt != null && !opt.trim().isEmpty() &&
                value != null && !value.trim().isEmpty();
    }
}
