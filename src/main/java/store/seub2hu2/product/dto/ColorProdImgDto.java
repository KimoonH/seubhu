package store.seub2hu2.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import store.seub2hu2.product.vo.Image;
import store.seub2hu2.product.vo.Product;

import java.util.List;

@NoArgsConstructor
@ToString
@Setter
@Getter
public class ColorProdImgDto {

    private int colorNo; // 색상번호
    private String colorName; // 색상명
    private Product product; // 상품 객체
    private List<Image> images; // 이미지 URL들
}
