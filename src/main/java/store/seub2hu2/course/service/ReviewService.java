package store.seub2hu2.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.admin.mapper.AdminMapper;
import store.seub2hu2.course.dto.AddReviewForm;
import store.seub2hu2.course.exception.CourseReviewException;
import store.seub2hu2.course.mapper.ReviewMapper;
import store.seub2hu2.course.vo.Course;
import store.seub2hu2.course.vo.Review;
import store.seub2hu2.course.vo.ReviewImage;
import store.seub2hu2.user.vo.User;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.Pagination;
import store.seub2hu2.util.S3Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReviewService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${upload.directory.course}")
    private String folder;

    //@Autowired
    //private S3Service s3Service;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private AdminMapper adminMapper;

    /**
     * 코스에 등록된 리뷰 목록을 가져온다.
     * @param condition 페이지, 코스 번호
     * @return 리뷰 목록
     */
    public ListDto<Review> getReviews(Map<String, Object> condition) {
        // 1. 코스에 해당하는 전체 리뷰의 개수를 가져온다.
        int totalRows = reviewMapper.getTotalRows(condition);

        // 2. Pagination 객체에 페이징 처리 정보를 저장한다.
        int page = (Integer) condition.get("page");
        Pagination pagination = new Pagination(page, totalRows, 5);

        // 3. 데이터 검색 범위를 조회해서 Map에 저장한다.
        condition.put("begin", pagination.getBegin());
        condition.put("end", pagination.getEnd());

        // 4. 조회범위에 맞는 리뷰 목록을 조회한다.
        List<Review> reviews = reviewMapper.getReviewsByNo(condition);

        // 5. 첨부 파일 목록이 있다면, 리뷰 객체에 저장한다.
        for (Review review : reviews) {
            int reviewNo = review.getNo();
            List<ReviewImage> reviewImages  = reviewMapper.getReviewImagesByNo(reviewNo);
            if (reviewImages.getFirst().getNo() != 0) {
                review.setReviewImage(reviewImages);
            }
        }

        // 6. ListDto 객체에 화면에 표시할 데이터(리뷰 목록, 페이징 처리 정보)를 담고, 반환한다.
        ListDto<Review> dto = new ListDto<>(reviews, pagination);
        return dto;
    }

    /**
     * 입력한 리뷰 정보를 등록한다.
     * @param form 입력한 리뷰 정보
     * @param userNo 사용자 번호
     * @return 입력한 리뷰 정보
     */
    public Review addNewReview(AddReviewForm form, int userNo) {
        // 1. 등록할 리뷰 정보를 리뷰 객체에 저장한다.
        Review review = new Review();
        review.setTitle(form.getTitle());
        review.setContent(form.getContent());
        review.setCreatedDate(new Date());

        Course course = new Course();
        course.setNo(form.getCourseNo());
        review.setCourse(course);

        User user = adminMapper.getUserByNo(userNo);
        review.setUser(user);

        // 2. 입력한 리뷰를 테이블에 저장한다.
        reviewMapper.insertReview(review);

        // 3. 첨부 파일이 있다면 if문을 실행한다.
        List<MultipartFile> multipartFiles = form.getUpfiles();
        List<ReviewImage> reviewImages = new ArrayList<>();

        if (multipartFiles != null) {
            for (MultipartFile multipartFile : multipartFiles) {
                // 첨부파일을 지정된 경로에 저장한다.
                String filename = System.currentTimeMillis() + multipartFile.getOriginalFilename();
                //s3Service.uploadFile(multipartFile, bucketName, folder, filename);

                // ReviewImage 객체에 파일 정보를 저장하고, 테이블에도 저장한다.
                ReviewImage reviewImage = new ReviewImage();
                reviewImage.setName(filename);
                reviewImage.setReviewNo(review.getNo());

                reviewMapper.insertReviewImage(reviewImage);

                // 리뷰 이미지 객체에 리뷰 이미지 정보를 저장한다.
                reviewImages.add(reviewImage);
            }
            review.setReviewImage(reviewImages);
        }

        // 4. 등록한 리뷰 정보를 반환한다.
        return review;
    }

    /**
     * 리뷰를 삭제한다.
     * @param reviewNo 리뷰 번호
     * @param userNo 사용자 번호
     */
    public void deleteReview(int reviewNo, int userNo) {
        // 1. 리뷰 번호에 해당하는 리뷰의 정보를 가져온다.
        Review review = reviewMapper.getReviewByNo(reviewNo);

        // 2. 리뷰 작성자와 로그인한 사용자가 동일한지 확인한다.
        if (review.getUser().getNo() != userNo) {
            throw new CourseReviewException("해당 리뷰 작성자만 삭제 가능합니다.");
        }

        // 3. 테이블에서 리뷰를 삭제한다.
        reviewMapper.deleteReview(reviewNo);

        // 4. 버킷에 저장된 리뷰 이미지를 삭제한다.
        List<ReviewImage> reviewImages = reviewMapper.getReviewImagesByNo(reviewNo);
        for (ReviewImage reviewImage : reviewImages) {
            //s3Service.deleteFile(bucketName, folder, reviewImage.getName());
        }
    }
}