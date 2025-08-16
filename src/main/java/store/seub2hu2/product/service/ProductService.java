package store.seub2hu2.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.order.exception.DatabaseSaveException;
import store.seub2hu2.order.exception.OutOfStockException;
import store.seub2hu2.order.exception.ProductNotFoundException;
import store.seub2hu2.order.exception.StockInsufficientException;
import store.seub2hu2.order.vo.OrderItem;
import store.seub2hu2.product.dto.*;
import store.seub2hu2.product.dto.condition.ProductSearchCondition;
import store.seub2hu2.product.dto.request.ProductSearchRequest;
import store.seub2hu2.product.mapper.ProductMapper;
import store.seub2hu2.product.vo.Product;
import store.seub2hu2.product.vo.Size;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.Pagination;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {


    private final ProductMapper productMapper;

    public void updateProdDetailViewCnt(int prodNo, int colorNo) {
         Product product = productMapper.getProductByProdNoAndColoNo(prodNo, colorNo);
         product.setCnt(product.getCnt() + 1);
         productMapper.incrementViewCount(product);
    }

    /**
     * 상품 전체 조회 API
     * @param request
     * @return
     */
    public ListDto<ProdListDto> getProducts(ProductSearchRequest request) {

        int totalRows = productMapper.getTotalRows(request);

        Pagination pagination = new Pagination(request.getPage(), totalRows, request.getRows());

        ProductSearchCondition condition = ProductSearchCondition.from(request, pagination);

        // ProdListDto 타입의 데이터를 담는 ListDto 객체를 생성한다.
        // 상품 목록 ListDto(ProdListDto), 페이정처리 정보(Pagination)을 담는다.
        List<ProdListDto> products = productMapper.getProducts(condition);
        log.debug("조회된 상품 수: {}", products.size());

        return new ListDto<>(products, pagination);
    }

    public ProductDetailBundle getProductDetailBundle(int productNo, int colorNo) {

        // 🔥 기존 XML에 있는 쿼리들 그대로 사용
        ProdDetailDto product = productMapper.getProduct(productNo);  // ✅ 이미 있음

        // 🔥 나머지는 일단 기존 Service 메서드들 복원
        List<ColorProdImgDto> colorOptions = getProdImgByProductNo(productNo);  // 기존 Service 메서드
        SizeAmountDto sizeAmount = getSizeAmountByColorNo(colorNo);             // 기존 Service 메서드
        ProdImagesDto selectedImages = getImagesByColorNo(colorNo);             // 기존 Service 메서드

        return new ProductDetailBundle(product, colorOptions, sizeAmount, selectedImages);
    }

    public List<ColorProdImgDto> getProdImgByProductNo(int productNo) {
        return productMapper.getProdImgByProductNo(productNo);  // 기존 XML 사용
    }

    public SizeAmountDto getSizeAmountByColorNo(int colorNo) {
        return productMapper.getSizeAmountByColorNo(colorNo);  // 기존 XML 사용
    }

    public ProdImagesDto getImagesByColorNo(int colorNo) {
        return productMapper.getProdImagesByColorNo(colorNo);  // 기존 XML 사용
    }

    /**
     * 개별 상품 정보 조회 (주문 등에서 사용)
     * @param productNo 상품 번호
     * @return 상품 상세 정보
     */
    public ProdDetailDto getProduct(int productNo) {
        return productMapper.getProduct(productNo);
    }

    /**
     * 주문 상품 목록으로부터 표시용 상품명을 생성합니다.
     * @param orderItems 주문 상품 목록
     * @return "상품명" 또는 "상품명 외 N개" 형태의 문자열
     */
    public String generateOrderItemName(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 상품 목록이 비어있습니다.");
        }

        int prodNo = orderItems.get(0).getProdNo();
        ProdDetailDto prodDetailDto = productMapper.getProduct(prodNo);

        if (prodDetailDto == null) {
            throw new ProductNotFoundException("상품 번호 " + prodNo + "에 대한 정보가 없습니다.");
        }

        String itemName = prodDetailDto.getProductName();

        if (orderItems.size() > 1) {
            itemName = itemName + " 외 " + (orderItems.size() - 1) + "개";
        }

        return itemName;
    }

    /**
     * 주문 상품들의 재고를 확인하고 업데이트합니다.
     * @param orderItems 주문 상품 목록
     * @param orderNo 주문 번호
     * @param itemName 상품명 (에러 메시지용)
     */
    public void validateAndUpdateStock(List<OrderItem> orderItems, int orderNo, String itemName) {
        for(OrderItem item : orderItems) {
            Size size = productMapper.getSizeAmount(item.getSizeNo());

            // 주문 상품의 재고를 확인한다.
            if(size.getAmount() == 0) {
                throw new OutOfStockException("상품" + item.getSizeNo() +"는 재고가 없습니다.");
            }

            // 재고가 부족한 경우 StockInsufficientException을 던집니다.
            if (size.getAmount() < item.getStock()) {
                throw new StockInsufficientException("상품 " + itemName + item.getSizeNo() + "의 재고가 부족합니다. 요청한 수량: "
                        + item.getStock() + ", 남은 재고: " + size.getAmount());
            }

            // OrderItem 설정
            item.setNo(item.getNo());
            item.setOrderNo(orderNo);
            item.setProdNo(item.getProdNo());
            item.setSizeNo(item.getSizeNo());
            item.setPrice(item.getPrice());
            item.setStock(item.getStock());
            item.setEachTotalPrice(item.getPrice() * item.getStock());

            // 주문 상품에 대한 재고를 감소한다.
            size.setAmount(size.getAmount() - item.getStock());

            try {
                productMapper.updateAmount(size);
            } catch (Exception ex) {
                throw new DatabaseSaveException("주문 상품의 재고 업데이트 실패", ex);
            }
        }
    }

}
