package store.seub2hu2.product.service;

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
public class ProductService {

    @Autowired
    ProductMapper productMapper;

    public void updateProdDetailViewCnt(int prodNo, int colorNo) {
         Product product = productMapper.getProductByProdNoAndColoNo(prodNo, colorNo);
         product.setCnt(product.getCnt() + 1);
         productMapper.incrementViewCount(product);
    }

    /**
     * 색상 번호에 따른 다양한 이미지 조회하기
     * @param colorNo 색상 번호
     * @return 해당 상품의 하나의 색상의 여러 이미지들 값
     */
    public ProdImagesDto getProdImagesByColorNo(int colorNo) {

        ProdImagesDto prodImagesDto = productMapper.getProdImagesByColorNo(colorNo);

        return prodImagesDto;
    }

    /**
     * 색상 번호에 따른 사이즈와 재고량 조회하기
     * @param colorNo 색상 번호
     * @return 사이즈와 재고수량
     */
    public SizeAmountDto getSizeAmountByColorNo(int colorNo) {

        SizeAmountDto getSizeAmount = productMapper.getSizeAmountByColorNo(colorNo);

        return getSizeAmount;
    }

    /**
     * 상품 번호에 따른 다양한 색 그리고 여러 이미지 중 대표 이미지 조회하기
     * @param no 상품 번호
     * @return 다양한 색, 대표 이미지 하나
     */
    public List<ColorProdImgDto> getProdImgByColorNo(int no) {

        List<ColorProdImgDto> colorImgByNo = productMapper.getProdImgByColorNo(no);

        return colorImgByNo;
    }

    /**
     * 개별 상품 정보 조회
     * @param no 상품 번호
     * @return 상품 상세 정보
     */
    public ProdDetailDto getProductByNo(int no) {

        ProdDetailDto prodDetailDto = productMapper.getProductByNo(no);

        return prodDetailDto;
    }

    /**
     * 모든 상품정보 목록을 제공하는 서비스 메서드입니다.
     * @param condition 조회조건이 포함된 MAP 객체입니다.
     * @return 모든 상품 목록
     */
    public ListDto<ProdListDto> getProducts(Map<String, Object> condition) {

        // 검색 조건에 맞는 전체 데이터 갯수를 조회하는 기능
        int totalRows = productMapper.getTotalRows(condition);

        // Pagination 객체를 생성한다.
        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");

        Pagination pagination = new Pagination(page, totalRows, rows);

        // 데이터 검색 범위를 조회해서 Map에 저장한다.
        condition.put("begin", pagination.getBegin());
        condition.put("end", pagination.getEnd());

        // ProdListDto 타입의 데이터를 담는 ListDto 객체를 생성한다.
        // 상품 목록 ListDto(ProdListDto), 페이정처리 정보(Pagination)을 담는다.
        List<ProdListDto> products = productMapper.getProducts(condition);
        System.out.println(products.toString());
        ListDto<ProdListDto> dto = new ListDto<>(products, pagination);

        return dto;
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
        ProdDetailDto prodDetailDto = productMapper.getProductByNo(prodNo);

        if (prodDetailDto == null) {
            throw new ProductNotFoundException("상품 번호 " + prodNo + "에 대한 정보가 없습니다.");
        }

        String itemName = prodDetailDto.getName();

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
