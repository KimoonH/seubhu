package store.seub2hu2.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.common.PerformanceLog;
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
     * 상품 전체 조회 API - 복잡한 검색이므로 여유있는 임계값
     */
    @PerformanceLog(
            value = "상품 전체 조회 API",
            warnThreshold = 1500,     // 1.5초 - 검색 조건, 페이징 등 고려
            errorThreshold = 5000,    // 5초 - 복잡한 쿼리 허용
            includeParams = true,     // 검색 조건 확인 중요
            includeResult = false     // 리스트는 크므로 결과 로깅 제외
    )
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

    /**
     * 상품 상세 Bundle 조회 - 핵심 API이므로 보통 수준의 임계값
     */
    @PerformanceLog(
            value = "상품 상세 Bundle 조회",
            warnThreshold = 1,     // 1초 - 사용자 경험 중요
            errorThreshold = 3000,    // 3초 - 너무 느리면 이탈
            includeParams = true,     // 어떤 상품/색상인지 추적 중요
            includeResult = false     // Bundle 객체는 크므로 제외
    )
    public ProductDetailBundle getProductDetailBundle(int productNo, int colorNo) {

        ProductDetailBundle productWithColors = productMapper.getProductWithAllColors(productNo);

        ColorDetailsDto colorDetails = productMapper.getColorDetails(colorNo);


        return new ProductDetailBundle(
                productWithColors.getProduct()
                , productWithColors.getColorOptions()
                , colorDetails.getSizeAmount()
                , colorDetails.getImages());
    }

    @PerformanceLog("상품 기본정보 조회")
    public ProdDetailDto getProduct(int productNo) {
        return productMapper.getProduct(productNo);
    }

    @PerformanceLog("색상 옵션 조회")
    public List<ColorProdImgDto> getProdImgByProductNo(int productNo) {
        return productMapper.getProdImgByProductNo(productNo);
    }

    @PerformanceLog("사이즈/재고 조회")
    public SizeAmountDto getSizeAmountByColorNo(int colorNo) {
        return productMapper.getSizeAmountByColorNo(colorNo);
    }

    @PerformanceLog("이미지 조회")
    public ProdImagesDto getImagesByColorNo(int colorNo) {
        return productMapper.getProdImagesByColorNo(colorNo);
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
