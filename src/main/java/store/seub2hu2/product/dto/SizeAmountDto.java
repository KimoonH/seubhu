package store.seub2hu2.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import store.seub2hu2.product.vo.Size;

import java.util.List;


@NoArgsConstructor
@ToString
@Setter
@Getter
public class SizeAmountDto {

    private int colorNo;        // 해당 색상의 번호 (이 색상에 속한 사이즈들이니까)
    private String colorName;   // 해당 색상의 이름
    private List<Size> sizes; //사이즈별 사이즈
}
