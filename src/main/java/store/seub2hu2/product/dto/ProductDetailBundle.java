package store.seub2hu2.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailBundle {

    // 상품 기본 정보
    private ProdDetailDto product;

    // 해당 상품의 모든 색상 옵션들 (색상별 대표 이미지 포함)
    private List<ColorProdImgDto> colorOptions;

    // 선택된 색상의 사이즈/재고 정보
    private SizeAmountDto sizeAmount;

    // 선택된 색상의 모든 이미지들
    private ProdImagesDto selectedColorImages;

    public ProdDetailDto getProdDetailDto() {
        return product;
    }

    public List<ColorProdImgDto> getColorProdImgDto() {
        return colorOptions;
    }

    public SizeAmountDto getSizeAmountDto() {
        return sizeAmount;
    }

    public ProdImagesDto getProdImagesDto() {
        return selectedColorImages;
    }

    public boolean hasColorOptions() {
        return colorOptions != null && !colorOptions.isEmpty();
    }

    public boolean hasSizes() {
        return sizeAmount != null && sizeAmount.getSizes() != null && !sizeAmount.getSizes().isEmpty();
    }

    public boolean hasImages() {
        return selectedColorImages != null && selectedColorImages.getImages() != null && !selectedColorImages.getImages().isEmpty();
    }
}
