package store.seub2hu2.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColorDetailsDto {

    private SizeAmountDto sizeAmount;      // 사이즈/재고 정보
    private ProdImagesDto images;          // 해당 색상의 모든 이미지
}
