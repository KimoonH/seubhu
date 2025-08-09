package store.seub2hu2.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.course.vo.ReviewImage;
import store.seub2hu2.product.dto.ProdReviewDto;
import store.seub2hu2.product.dto.ProdReviewForm;
import store.seub2hu2.product.mapper.ProdReviewMapper;
import store.seub2hu2.product.mapper.ProductMapper;
import store.seub2hu2.product.vo.ProdReview;
import store.seub2hu2.product.vo.ProdReviewImg;
import store.seub2hu2.product.vo.Product;
import store.seub2hu2.util.S3Service;

import java.util.*;

@RequiredArgsConstructor
@Service
@Transactional
public class ProdReviewService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    private final ProdReviewMapper prodReviewMapper;
    private final ProductMapper productMapper;


    /**
     * 상품 리뷰들 조회한다.
     * @param prodNo 상품 번호
     * @return 해당 상품의 리뷰들
     */
    public List<ProdReviewDto> getProdReviews(int prodNo) {

        List<ProdReviewDto> reviews = prodReviewMapper.prodReviewDto(prodNo);

        return reviews;
    }

    /**
     * 해당 리뷰의 값을 조회한다.
     * @param reviewNo 리뷰 번호
     * @return 해당 리뷰의 정보
     */
    public ProdReview getProdReviewByNo(int reviewNo) {
        return prodReviewMapper.getProdReviewByNo(reviewNo);
    }

    /**
     * 리뷰를 저장한다.
     * @param form 리뷰 정보
     * @param userNo 유저 번호
     * @return 리뷰 정보
     */
    public ProdReviewDto addProdReview(ProdReviewForm form, int userNo) {

        ProdReviewDto dto = new ProdReviewDto();

        ProdReview prodReview = new ProdReview();
        prodReview.setProdNo(form.getProdNo());
        prodReview.setTitle(form.getTitle());
        prodReview.setContent(form.getContent());
        prodReview.setUserNo(userNo);
        prodReview.setRating(form.getRating());

        prodReviewMapper.insertProdReview(prodReview);


        Product product = productMapper.getProductByProdNoAndColoNo(form.getProdNo(), form.getColorNo());

        // 3. 첨부 파일이 있다면 if문을 실행한다.
        List<MultipartFile> multipartFiles = form.getReviewFiles();
        List<ProdReviewImg> prodReviewImgs = new ArrayList<>();

        if (multipartFiles != null) {
            for (MultipartFile multipartFile : multipartFiles) {
                // 첨부파일을 지정된 경로에 저장한다.
                String filename = System.currentTimeMillis() + multipartFile.getOriginalFilename();
                //s3Service.uploadFile(multipartFile, bucketName, folder, filename);

                // ReviewImage 객체에 파일 정보를 저장하고, 테이블에도 저장한다.
                ProdReviewImg prodImg = new ProdReviewImg();
                prodImg.setReviewNo(prodReview.getNo());
                prodImg.setImgName(filename);

                prodReviewMapper.insertProdReviewsImg(prodImg);

                // 리뷰 이미지 객체에 리뷰 이미지 정보를 저장한다.
                prodReviewImgs.add(prodImg);
            }

            dto.setProdReviewImgs(prodReviewImgs);
        }

        // 5. 리뷰 평점 평균 계산
        double avgRating = prodReviewMapper.calculateAverageRatingByProdNo(form.getProdNo());
        productMapper.updateProductAvgRating(form.getProdNo(), avgRating);

        dto.setProdName(product.getName());
        dto.setColorName(product.getColor().getName());
        dto.setReviewNo(prodReview.getNo());
        dto.setReviewTitle(prodReview.getTitle());
        dto.setReviewContent(prodReview.getContent());
        dto.setRating(prodReview.getRating());
        dto.setProdNo(prodReview.getProdNo());
        dto.setReviewDate(new Date());


        return dto;
    }

    /**
     * 리뷰 정보 수정
     * @param prodReview 리뷰 객체
     */
    public void updateProdReview(ProdReview prodReview) {
        if(prodReview.getNo() == 0) {
            throw new IllegalArgumentException("리뷰 번호가 필요합니다.");
        }

        prodReview.setUpdatedDate(new Date());
        prodReviewMapper.updateProdReview(prodReview);
    }

    /**
     * 리뷰 삭제
     * @param reviewNo 리뷰 번호
     * @return 참이면 삭제
     */
    public boolean deleteProdReview(int reviewNo) {

        try {
            prodReviewMapper.deleteProdReview(reviewNo);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
