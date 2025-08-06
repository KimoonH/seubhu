package store.seub2hu2.admin.service;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.admin.dto.*;
import store.seub2hu2.admin.mapper.AdminMapper;
import store.seub2hu2.course.mapper.CourseMapper;
import store.seub2hu2.course.vo.Course;
import store.seub2hu2.course.vo.Region;
import store.seub2hu2.lesson.mapper.LessonMapper;
import store.seub2hu2.lesson.vo.Lesson;
import store.seub2hu2.product.dto.ProdListDto;
import store.seub2hu2.product.mapper.ProductMapper;
import store.seub2hu2.product.vo.*;
import store.seub2hu2.user.mapper.UserMapper;
import store.seub2hu2.user.vo.User;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.Pagination;
import store.seub2hu2.util.S3Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AdminService {

    @Value("${project.upload.path}")
    private String saveDirectory;
    @Value("${upload.directory.product}")
    private String saveProductDirectory;
//
//    private final CommunityMapper communityMapper;
//
//    private final CourseMapper courseMapper;
//
//    private final OrderMapper orderMapper;
//
//    private final ProductMapper productMapper;
//

     //S3 관련 임시 주석 처리
     @Value("${cloud.aws.s3.bucket}")
     private String bucketName;

    @Autowired
    private LessonMapper lessonMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ProductMapper productMapper;


    public List<Lesson> getLessons(Map<String, Object> condition) {

        List<Lesson> lessons = adminMapper.getAllLessons(condition);

        return lessons;

    }

    @Autowired
    private UserMapper userMapper;

    public ListDto<User> getAllUsers(Map<String, Object> condition) {

            int totalRows = adminMapper.getTotalRows(condition);

            int page = (Integer) condition.get("page");
            int rows = (Integer) condition.get("rows");
            Pagination pagination = new Pagination(page, totalRows, rows);

            int begin = pagination.getBegin();
            int end = pagination.getEnd();
            condition.put("begin", begin);
            condition.put("end", end);


            List<User> users = adminMapper.getUsers(condition);

            ListDto<User> dto = new ListDto<>(users, pagination);

            return dto;
        }

    public User getUser(@Param("no") int no) {

            return adminMapper.getUserByNo(no);
    }
    // 상품 수정


    // 상품번호로 그 상품에 관련된 모든 정보 조회
    public Product getProductNo(int no) {

        Product product = adminMapper.getProductByNo(no);

        return product;

    }

    // 상품등록
    public void addProduct(ProductRegisterForm form) {

        HashMap<String, Object> condition = new HashMap<String, Object>();

        condition.put("name", form.getName());
        condition.put("price", form.getPrice());
        condition.put("brandNo", form.getBrandNo());
        condition.put("categoryNo", form.getCategoryNo());
        condition.put("content", form.getContent());
        condition.put("thumbnail", form.getThumbnail());

        adminMapper.insertProduct(condition);
    }

    // 카테고리 조회
    public Category getCategory(int categoryNo) {
        return adminMapper.getTopCategoryNo(categoryNo);
    }

    // 컬러 번호 조회
    public int getColor(Map<String, Object> condition) {

        Integer colorNo = adminMapper.getColorNo((HashMap<String, Object>) condition);
        return Optional.ofNullable(colorNo).orElse(0); // 결과가 없으면 0 반환
    }

    // 상품 컬러 추가
    public void addColor(Map<String, Object> condition) {

        adminMapper.insertColor((HashMap<String, Object>) condition);
    }

    public void getUpdateProduct(Product product) {

        adminMapper.updateProduct(product);
    }

    public List<Color> getColorName(int no) {
        return adminMapper.colorNames(no);
    }

    public void addThumb(ColorThumbnailForm form, List<String> links) {


        int index = 0;
        for (String link: links) {

            Image img = new Image();
            img.setProdNo(form.getProdNo());
            img.setColorNo(form.getColorNo());
            img.setUrl(link);


            adminMapper.insertImage(img);
        }

    }

    public List<Image> getImageByColorNo(Integer colorNo) {

        return adminMapper.getImageByColorNum(colorNo);
    }

    public Color getColorNo(int colorNo) {
        return adminMapper.getColorNoByNo(colorNo);
    }

    public void getThumbnailByNo(Integer imgNo) {
        adminMapper.getIsThumByNo(imgNo);
    }

    public void getNullImageThumbyimgNo(Integer imgNo) {
        adminMapper.getNullImageThum(imgNo);
    }

    public void getEditUrl(List<Integer> imgNos, List<String> urls) {

        for (int i = 0; i < imgNos.size(); i++) {
            Image img = new Image();
            img.setNo(imgNos.get(i));
            img.setUrl(urls.get(i));

            adminMapper.editUrl(img);
        }
    }

    public List<Size> getAllSizeByColorNo(Integer colorNo) {

        List<Size> sizeList = adminMapper.getAllSizesByColorNo(colorNo);

        return sizeList;

    }

    public void getCheckSize(Map<String, Object> condition) {
        Size size = new Size();
        size.setSize((String) condition.get("size"));

        Color color = new Color();
        color.setNo((Integer) condition.get("colorNo"));
        size.setColor(color);

        Size existingSize = adminMapper.getCheckSizeByCon(size);

        if (existingSize != null && existingSize.getIsDeleted().equals("N")) {
            throw new IllegalArgumentException("이미 등록된 사이즈입니다.");

        } else if (existingSize != null && existingSize.getIsDeleted().equals("Y")) {
            adminMapper.getChangeIsDeleted(size);
        } else {
            adminMapper.getInsertSize(size);
        }
    }

    public void getDeletedSize(int sizeNo) {

        adminMapper.getDeleteSize(sizeNo);
    }

    public List<Color> getStockByColorNum(Map<String, Object> condition) {

        List<Color> dto = adminMapper.getStockByColorNumber(condition);

        return dto;
    }

    public void getInsertStock(Map<String, Object> condition) {

        adminMapper.insertStock(condition);
    }

    public List<LessonUsersDto> getLessonUser(Integer lessonNo) {

        List<LessonUsersDto> dto = adminMapper.getLessonUserByNo(lessonNo);

        return dto;
    }

    public ListDto<SettlementDto> getSettleList(Map<String, Object> condition) {

        int totalRows = adminMapper.getSettleTotalRows(condition);


        System.out.println("----------------------------------------------------totalRows: " + totalRows);

        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");

        Pagination pagination = new Pagination(page, totalRows, rows);
        int begin = pagination.getBegin();
        int end = pagination.getEnd();
        condition.put("begin", begin);
        condition.put("end", end);

        List<SettlementDto> settlementDtos = adminMapper.getSettleLists(condition);

        ListDto<SettlementDto> dto = new ListDto<>(settlementDtos, pagination);

        return dto;
    }

    // 코스 등록 전 새로운지역인지 기존지역인지 확인 후 등록
    public void checkNewRegion (CourseRegisterForm form) {
        Region region = new Region();
        region.setSi(form.getSi());
        region.setGu(form.getGu());
        region.setDong(form.getDong());

        Region savedRegion = adminMapper.checkRegion(region);

        Course course = new Course();

        if (savedRegion == null) {
            adminMapper.insertRegion(region);

            course.setRegion(adminMapper.getRegions(region));

            course.setName(form.getName());
            course.setTime(form.getTime());
            course.setLevel(form.getLevel());
            Double distance = form.getDistance();

            if (distance == null) {
                distance = 0.0; // 기본값 설정 (필요에 따라 변경)
            }
            course.setDistance(distance);

            MultipartFile multipartFile = form.getImage();
            if (multipartFile != null) {
                String originalFilename = multipartFile.getOriginalFilename();
                String filename = System.currentTimeMillis() + originalFilename;


                //FileUtils.saveMultipartFile(multipartFile, saveDirectory, filename);
                // s3Service.uploadFile(multipartFile, bucketName, saveDirectory, filename);
                // S3 임시 비활성화 - 파일명만 설정

                course.setFilename(filename);
            }

            adminMapper.insertCourse(course);
        } else {


            course.setRegion(savedRegion);
            course.setName(form.getName());
            course.setTime(form.getTime());
            course.setLevel(form.getLevel());
            Double distance = form.getDistance();

            if (distance == null) {
                distance = 0.0; // 기본값 설정 (필요에 따라 변경)
            }
            course.setDistance(distance);

            MultipartFile multipartFile = form.getImage();
            if (multipartFile != null) {
                String originalFilename = multipartFile.getOriginalFilename();
                String filename = System.currentTimeMillis() + originalFilename;

                //FileUtils.saveMultipartFile(multipartFile, saveDirectory, filename);
                // s3Service.uploadFile(multipartFile, bucketName, saveDirectory, filename);
                // S3 임시 비활성화 - 파일명만 설정

                course.setFilename(filename);
            }

            adminMapper.insertCourse(course);
        }

    }

    public Course getCourseByNo(int courseNo) {

        return adminMapper.getCourseByNos(courseNo);
    }

    public void getUpdateCourse(CourseRegisterForm form) {
        MultipartFile multipartFile = form.getImage();

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("이미지가 업로드되지 않았습니다.");
        }

        // 기존 코드 유지
        Region region = new Region();
        region.setSi(form.getSi());
        region.setGu(form.getGu());
        region.setDong(form.getDong());

        Region savedRegion = adminMapper.checkRegion(region);
        Course course = new Course();

        if (savedRegion == null) {
            adminMapper.insertRegion(region);
            course.setRegion(adminMapper.getRegions(region));
        } else {
            course.setRegion(savedRegion);
        }

        course.setNo(form.getNo());
        course.setName(form.getName());
        course.setTime(form.getTime());
        course.setLevel(form.getLevel());
        Double distance = form.getDistance() != null ? form.getDistance() : 0.0;
        course.setDistance(distance);

        String originalFilename = multipartFile.getOriginalFilename();
        String filename = System.currentTimeMillis() + originalFilename;

        //s3Service.uploadFile(multipartFile, bucketName, saveDirectory, filename);
        course.setFilename(filename);

        adminMapper.updateCourse(course);
    }

    public Map<String, Object> getTotalSubject(String day) {

        int breath = adminMapper.getTotalBreath(day);

        int action = adminMapper.getTotalAction(day);

        int exercise = adminMapper.getTotalExercise(day);

        Map<String, Object> condition = new HashMap<>();
        condition.put("breath", breath);
        condition.put("action", action);
        condition.put("exercise", exercise);

        return condition;
    }

    public Map<String, Object> getTotalPrice(String yesterday) {

        int totalPrice = adminMapper.getTotalPriceByDay(yesterday);


        Map<String, Object> condition = new HashMap<>();
        condition.put("totalPrice", totalPrice);

        return condition;
    }

    public ListDto<OrderProductDto> getOrderProduct(Map<String, Object> condition) {

        int totalRows = adminMapper.getTotalOrderProd(condition);

        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");

        Pagination pagination = new Pagination(page, totalRows, rows);
        int begin = pagination.getBegin();
        int end = pagination.getEnd();
        condition.put("begin", begin);
        condition.put("end", end);

        List<OrderProductDto> orderProductDtos = adminMapper.getSettleProdList(condition);

        ListDto<OrderProductDto> dto = new ListDto<>(orderProductDtos, pagination);

        return dto;
    }

    public List<prevOrderProdDto> getOrderProdPrev(int orderNo) {

        List<prevOrderProdDto> dto = adminMapper.orderProdPrev(orderNo);

        return dto;
    }


    public Map<String, Object> getTotalProdPrice(String yesterday) {

        int totalProdPrice = adminMapper.getTotalProdPriceByDay(yesterday);
        Map<String, Object> condition = new HashMap<>();
        condition.put("totalProdPrice", totalProdPrice);

        return condition;
    }

    public Map<String, Object> getTotalProdAmount(String yesterday) {
        int totalProdAmount = adminMapper.getTotalOrderProdAmount(yesterday);



        Map<String, Object> condition = new HashMap<>();
        condition.put("totalProdAmount", totalProdAmount);

        return condition;
    }

    public Map<String, Object> getTotalProdCatNo(String day) {

        int man = adminMapper.getTotalProdMan(day);

        int woman = adminMapper.getTotalProdWoman(day);

        int run = adminMapper.getTotalProdRun(day);

        Map<String, Object> condition = new HashMap<>();
        condition.put("man", man);
        condition.put("woman", woman);
        condition.put("run", run);

        return condition;
    }

    public ListDto<ProdListDto> getStockProduct(Map<String, Object> condition) {

        // 검색 조건에 맞는 전체 데이터 갯수를 조회하는 기능
        int totalRows = productMapper.getTotalRows(condition);

        // Pagination 객체를 생성한다.
        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");

        Pagination pagination = new Pagination(page, totalRows, rows);

        // 데이터 검색 범위를 조회해서 Map에 저장한다.
        condition.put("begin", pagination.getBegin());
        condition.put("end", pagination.getEnd());

        List<ProdListDto> products = adminMapper.getStockProducts(condition);

        ListDto<ProdListDto> dto = new ListDto<>(products, pagination);

        return dto;
    }

    public ListDto<orderDeliveryDto> getOrderDelivery(Map<String, Object> condition) {

        int totalRows = adminMapper.getDeliveryTotalRows(condition);

        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");

        Pagination pagination = new Pagination(page, totalRows, rows);

        int begin = pagination.getBegin();
        int end = pagination.getEnd();

        // 데이터 검색 범위를 조회해서 Map에 저장한다.
        condition.put("begin", begin);
        condition.put("end", end);

        List<orderDeliveryDto> deliveryDtos = adminMapper.getOrderDeliveries(condition);


        ListDto<orderDeliveryDto> dtos = new ListDto<>(deliveryDtos, pagination);

        return dtos;
    }

    public void getDeletedProd(Map<String, Object> condition) {

        adminMapper.getDeletedProds(condition);
    }

    public void updateProductShowStatus(Map<String, Object> condition) {

        adminMapper.getUpdateShows(condition);
    }

    public void getDeletedCourse(int courseNo) {

        adminMapper.getDeletedCourses(courseNo);
    }

    public void getUpdateDelivery(Map<String, Object> condition) {

        if(condition.get("deliStatus").equals("배송준비중")) {

            adminMapper.getUpdateDeliverySetReady(condition);

        } else if (condition.get("deliStatus").equals("배송출발")) {

            adminMapper.getUpdateDeliverySetShipped(condition);

        } else if (condition.get("deliStatus").equals("배송완료")) {

            adminMapper.getUpdateDeliverySetDelivered(condition);
        }


    }

    public ListDto<ReportDto> getReport(Map<String, Object> condition) {

        int totalRows = adminMapper.getReportTotalRows(condition);

        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");

        Pagination pagination = new Pagination(page, totalRows, rows);

        int begin = pagination.getBegin();
        int end = pagination.getEnd();

        condition.put("begin", begin);
        condition.put("end", end);

        List<ReportDto> dto = adminMapper.getReports(condition);

        ListDto<ReportDto> dtos = new ListDto<>(dto, pagination);

         return dtos;
    }

    public void UpdateReport(Map<String, Object> condition) {

        if (condition.get("reportType").equals("board")){

            adminMapper.updateBoardStatus(condition);

            adminMapper.updateReportStatus(condition);
        } else if (condition.get("reportType").equals("boardReply")){

            adminMapper.updateBoardReplyStatus(condition);

            adminMapper.updateReportStatus(condition);
        } else if (condition.get("reportType").equals("crew")) {

            adminMapper.updateCrewStatus(condition);

            adminMapper.updateReportStatus(condition);
        } else if (condition.get("reportType").equals("crewReply")) {

            adminMapper.updateCrewReplyStatus(condition);

            adminMapper.updateReportStatus(condition);
        }
    }
}
