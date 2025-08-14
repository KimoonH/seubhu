package store.seub2hu2.product.dto.condition;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import store.seub2hu2.product.dto.request.ProductSearchRequest;
import store.seub2hu2.util.Pagination;

/**
 * 상품 검색 조건 DTO (Service → Mapper 전달용)
 * ProductSearchRequest + Pagination 정보를 합친 객체
 */
@Getter
@Builder
@ToString
public class ProductSearchCondition {

    // 검색 조건 (ProductSearchRequest에서 가져옴)
    private Integer topNo;
    private Integer catNo;
    private String sort;
    private String opt;
    private String value;

    // 페이지네이션 정보 (Pagination에서 가져옴)
    private Integer page;
    private Integer rows;
    private Integer begin;
    private Integer end;

    /**
     * ProductSearchRequest와 Pagination으로부터 조건 객체 생성
     */
    public static ProductSearchCondition from(ProductSearchRequest request, Pagination pagination) {
        return ProductSearchCondition.builder()
                .topNo(request.getTopNo())
                .catNo(request.hasCategoryFilter() ? request.getCatNo() : null)
                .sort(request.getSort())
                .opt(request.hasSearchCondition() ? request.getOpt() : null)
                .value(request.hasSearchCondition() ? request.getValue() : null)
                .page(request.getPage())
                .rows(request.getRows())
                .begin(pagination.getBegin())
                .end(pagination.getEnd())
                .build();
    }

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
