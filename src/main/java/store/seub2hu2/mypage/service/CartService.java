package store.seub2hu2.mypage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.cart.dto.CartItemDto;
import store.seub2hu2.cart.dto.CartRegisterForm;
import store.seub2hu2.cart.vo.Cart;
import store.seub2hu2.mypage.mapper.CartMapper;
import store.seub2hu2.order.vo.OrderItem;


import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartService {

    @Autowired
    CartMapper cartMapper;

    /**
     * 장바구니 상품을 삭제한다.
     * @param cartNoList 장바구비 번호들
     */
    public void deleteCartItems(List<Integer> cartNoList) {

        cartMapper.deleteCartItems(cartNoList);
    }

    /**
     * 사용자의 번호에 따른 장바구니 상품 정보와 수량 정보를 조회하는 기능
     * @param userNo 유저 번호
     * @return 상품 정보와 수량 정보
     */
    public List<CartItemDto> getCartItemsByUserNo(int userNo){

        List<CartItemDto> cartItems = cartMapper.getCartItemsByUserNo(userNo);

        return cartItems;
    }

    /**
     * 카트 정보를 입력하는 기능
     * @param cartRegisterForm 카트 DB에 넣을 정보
     */
    public void addCart(List<CartRegisterForm> cartRegisterForm) {

        for(CartRegisterForm form : cartRegisterForm){
            Cart cart = cartMapper.getCartByUserNoAndSizeNo(form.getUserNo(), form.getSizeNo());

            if(cart == null) {
                cartMapper.addCart(cartRegisterForm);
            } else {
                System.out.println("기존 상품 수량: " + cart.getAmount());
                System.out.println("증가시킬 수량: " + form.getStock());
                cart.setAmount(cart.getAmount() + form.getStock());
                cartMapper.updateCart(cart);
            }

        }
    }

    /**
     * 위시리스트에서 장바구니로 추가
     * @param cart
     */
    public void addToCart(Cart cart) {
        // Null 체크
        if (cart == null) {
            throw new IllegalArgumentException("Cart 객체가 null입니다.");
        }


        cartMapper.addToCart(cart);
    }

    /**
     * 주문 완료 후 장바구니에서 해당 상품들을 제거합니다.
     * @param orderItems 주문 상품 목록
     */
    public void removeOrderedItems(List<OrderItem> orderItems) {
        List<Integer> cartNos = new ArrayList<>();
        List<Integer> invalidCartNos = new ArrayList<>();

        for(OrderItem item : orderItems) {
            if(item.getCartNo() <= 0) {
                invalidCartNos.add(item.getCartNo());
            } else {
                cartNos.add(item.getCartNo());
            }
        }

        if(!invalidCartNos.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 장바구니 번호" + invalidCartNos);
        }

        if(!cartNos.isEmpty()) {
            cartMapper.deleteCartItems(cartNos);
        }
    }
}
