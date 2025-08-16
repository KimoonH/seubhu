package store.seub2hu2.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import store.seub2hu2.product.vo.*;



@NoArgsConstructor
@ToString
@Setter
@Getter
public class ProdDetailDto {
    private int productNo; // 상품 번호
    private String productName; // 상품명
    private String content; // 상품설명
    private int price; // 상품 가격
    private String status; // 상품 상태
    private int cnt; // 조회수
    private double rating; // 평점
    private int colorNum; // 대표 색상 번호
    private Category category; // 카테고리 객체
    private Brand brand; // 브랜드 객체
}
