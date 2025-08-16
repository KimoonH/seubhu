package store.seub2hu2.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import store.seub2hu2.product.dto.*;
import store.seub2hu2.product.dto.condition.ProductSearchCondition;
import store.seub2hu2.product.dto.request.ProductSearchRequest;
import store.seub2hu2.product.vo.Product;
import store.seub2hu2.product.vo.Size;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {

    // 평점 증가
    void updateProductAvgRating(@Param("prodNo") int prodNo, @Param("avgRating") Double avgRating);

    // 수량 증감
    void updateAmount(@Param("size") Size size);

    // 조회수 증가
    void incrementViewCount(@Param("product") Product product);

    // 상품 조회
    Product getProductByProdNoAndColoNo(@Param("prodNo") int prodNo, @Param("colorNo") int colorNo);

    // 수량 체크
    Size getSizeAmount(@Param("sizeNo") int sizeNo);

    // 옵션에 따른 데이터 전체 개수를 조회하기
    int getTotalRows(@Param("request") ProductSearchRequest request);

    // 상품 전체 목록 조회하기
    List<ProdListDto> getProducts(@Param("condition") ProductSearchCondition condition);


    /**
     * 상품 기본정보 + 모든 색상 옵션 조회 (1번째 통합 쿼리)
     */
    ProductDetailBundle getProductWithAllColors(int productNo);

    /**
     * 특정 색상의 상세정보 조회 (사이즈/재고 + 모든 이미지) (2번째 통합 쿼리)
     */
    ColorDetailsDto getColorDetails(int colorNo);

    ProdDetailDto getProduct(@Param("productNo") int productNo);
    List<ColorProdImgDto> getProdImgByProductNo(@Param("productNo") int productNo);  // 메서드명 변경
    ProdImagesDto getProdImagesByColorNo(@Param("colorNo") int colorNo);            // 메서드명 변경
    SizeAmountDto getSizeAmountByColorNo(@Param("colorNo") int colorNo);

}
