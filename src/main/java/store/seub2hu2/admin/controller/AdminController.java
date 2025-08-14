package store.seub2hu2.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import store.seub2hu2.admin.dto.*;
import store.seub2hu2.admin.service.AdminService;
import store.seub2hu2.community.service.MarathonService;
import store.seub2hu2.community.service.NoticeService;
import store.seub2hu2.community.vo.Marathon;
import store.seub2hu2.community.vo.Notice;
import store.seub2hu2.course.service.CourseService;
import store.seub2hu2.course.service.UserCourseService;
import store.seub2hu2.course.vo.Course;
import store.seub2hu2.lesson.dto.LessonRegisterForm;
import store.seub2hu2.lesson.dto.LessonUpdateForm;
import store.seub2hu2.lesson.service.LessonFileService;
import store.seub2hu2.lesson.service.LessonService;
import store.seub2hu2.lesson.vo.Lesson;
import store.seub2hu2.mypage.dto.AnswerDTO;
import store.seub2hu2.mypage.dto.QnaResponse;
import store.seub2hu2.mypage.service.QnaService;
import store.seub2hu2.product.dto.*;
import store.seub2hu2.product.service.ProductService;
import store.seub2hu2.product.vo.*;
import store.seub2hu2.product.vo.Category;
import store.seub2hu2.product.vo.Color;
import store.seub2hu2.product.vo.Image;
import store.seub2hu2.product.vo.Product;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.user.service.UserService;
import store.seub2hu2.user.vo.User;
import store.seub2hu2.util.ListDto;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CourseService courseService;
    private final AdminService adminService;
    private final LessonService lessonService;
    private final LessonFileService lessonFileService;
    private final ProductService productService;
    private final UserService userService;
    private final QnaService qnaService;
    private final UserCourseService userCourseService;
    private final MarathonService marathonService;
    private final NoticeService noticeService;

    @GetMapping("/home")
    public String home() {

        return "admin/home";
    }

    @GetMapping("/lesson-edit-form")
    public String lessonEditForm(@RequestParam("lessonNo") Integer lessonNo, Model model) {
        try {
            // 강사 정보 가져오기
            List<User> lecturers = userService.findUsersByUserRoleNo(3);

            // Lesson 정보 가져오기
            Lesson lesson = lessonService.getLessonByNo(lessonNo);
            LessonUpdateForm form = new LessonUpdateForm();
            form.setLessonNo(lessonNo);
            form.setTitle(lesson.getTitle());
            form.setPlan(lesson.getPlan());
            form.setPrice(lesson.getPrice());
            form.setSubject(lesson.getSubject());
            form.setStatus(lesson.getStatus());
            form.setStart(lesson.getStartDate() + " " + lesson.getStartTime());
            form.setEnd(lesson.getEndDate() + " " + lesson.getEndTime());

            // 이미지 파일 정보 가져오기
            Map<String, String> images = lessonFileService.getImagesByLessonNo(lessonNo);

            // 모델에 레슨, 이미지, 강사 정보 추가
            model.addAttribute("lecturers", lecturers);
            model.addAttribute("lesson", lesson);
            model.addAttribute("lessonNo", lessonNo);
            model.addAttribute("images", images);
            model.addAttribute("form", form);

            log.info("lesson start = {}", lesson);

            return "admin/lesson-edit-form";
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid lessonNo: " + lessonNo);
        }
    }

    @PostMapping("/lesson-edit-form")
    public String lessonEditForm(@Validated @ModelAttribute("form") LessonUpdateForm form, BindingResult result, Model model) {
        if (!StringUtils.hasText(form.getPlan()) && form.getMainImage().isEmpty()) {
            result.rejectValue("plan", null, "계획을 작성하거나 메인 이미지를 첨부 해주세요.");
            result.rejectValue("mainImage", null, "계획을 작성하거나 메인 이미지를 첨부 해주세요.");
        }

        if(form.getThumbnail().isEmpty()) {
            result.rejectValue("thumbnail", null, "썸네일을 첨부 해주세요.");
        }

        if (result.hasErrors()) {
            // 강사 정보 가져오기
            List<User> lecturers = userService.findUsersByUserRoleNo(3);

            // Lesson 정보 가져오기
            int lessonNo = form.getLessonNo();
            Lesson lesson = lessonService.getLessonByNo(lessonNo);

            // 이미지 파일 정보 가져오기
            Map<String, String> images = lessonFileService.getImagesByLessonNo(lessonNo);

            // 모델에 레슨, 이미지, 강사 정보 추가
            model.addAttribute("lecturers", lecturers);
            model.addAttribute("lesson", lesson);
            model.addAttribute("lessonNo", lessonNo);
            model.addAttribute("images", images);
            model.addAttribute("form", form);

            log.info("lesson start = {}", lesson);

            return "admin/lesson-edit-form";
        }

        log.info("레슨 수정 정보 {} ", form);
        lessonService.updateLesson(form);

        return "redirect:/admin/lesson";
    }

    //@ModelAttribute
    //public List<User> lectures() {
    //    return userService.findUsersByUserRoleNo(3);
    //}

    @GetMapping("/lesson-register-form")
    public String lessonRegisterForm(Model model) {

        // 사용자 권한이 강사인 사용자 목록을 조회한다.
        List<User> lecturers = userService.findUsersByUserRoleNo(3);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("form", new LessonRegisterForm());

        return "admin/lesson-register-form";
    }

    @PostMapping("/lesson-register-form")
    public String form(@Validated @ModelAttribute("form") LessonRegisterForm form, BindingResult result, Model model) throws IOException {

        if (!StringUtils.hasText(form.getPlan()) && form.getMainImage().isEmpty()) {
            result.rejectValue("plan", null, "계획을 작성하거나 메인 이미지를 첨부 해주세요.");
            result.rejectValue("mainImage", null, "계획을 작성하거나 메인 이미지를 첨부 해주세요.");
        }

        if(form.getThumbnail().isEmpty()) {
            result.rejectValue("thumbnail", null, "썸네일을 첨부 해주세요.");
        }

        if (result.hasErrors()) {
            // 강사 목록 전달
            List<User> lecturers = userService.findUsersByUserRoleNo(3);
            model.addAttribute("lecturers", lecturers);

            return "admin/lesson-register-form"; // 오류가 있는 경우 다시 폼 페이지로 리턴
        }

        lessonService.registerLesson(form);

        return "redirect:/admin/lesson";
    }

    @GetMapping("/lesson/preview")
    @ResponseBody
    public List<LessonUsersDto> lessonPreview(@RequestParam("no") Integer lessonNo,
                                              Model model) {

        List<LessonUsersDto> reservations = adminService.getLessonUser(lessonNo);


        return reservations;
    }

    @GetMapping("/lesson")
    public String lesson(@RequestParam(name = "opt", required = false) String opt,
                         @RequestParam(name = "day", required = false)
                         @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate day,
                         @RequestParam(name = "value", required = false) String value,
                         Model model) {

        if (day == null) {
            day = LocalDate.now();
        }

        String formattedDay = day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Map<String, Object> condition = new HashMap<>();
        condition.put("day", formattedDay);
        if (StringUtils.hasText(value)) {
            condition.put("opt", opt);
            condition.put("value", value);
        }

        List<Lesson> lessons = adminService.getLessons(condition);
        model.addAttribute("lessons", lessons);
        model.addAttribute("day", day); // 선택한 날짜를 다시 전달

        return "admin/lessonlist";
    }

    @PostMapping("/course-delete")
    public String courseDelete(@RequestParam("no") int courseNo){

        adminService.getDeletedCourse(courseNo);

        return "redirect:/admin/course";
    }

    @GetMapping("/course-edit-form")
    public String getCourseEditForm(@RequestParam("no") int courseNo, Model model) {

        Course course = adminService.getCourseByNo(courseNo);

        model.addAttribute("course", course);

        return "admin/course-edit-form";
    }

    @PostMapping("/course-edit-form")
    public String courseEditForm(@RequestParam Map<String, String> params,
                                @RequestParam("no") int courseNo, CourseRegisterForm form,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                RedirectAttributes redirectAttributes) {

//        try {
//            adminService.getUpdateCourse(form);
//        } catch (IllegalArgumentException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/admin/course-edit?no=" + courseNo;
//        }

        // 유효성 검사
        if (params.get("name").isEmpty() ||
                params.get("time").isEmpty() ||
                params.get("level").isEmpty() ||
                params.get("distance").isEmpty() ||
                (image == null || image.isEmpty())) {

            redirectAttributes.addFlashAttribute("errorMessage", "모든 입력값을 입력해주세요.");
            return "redirect:/admin/course-edit";
        }

        adminService.getUpdateCourse(form);

        return "redirect:/admin/course-detail?no=" + courseNo;
    }

    @GetMapping("/course-register-form")
    public String courseRegisterForm() {
        return "admin/course-register-form";
    }

    @PostMapping("/course-register-form")
    public String courseRegisterForm(CourseRegisterForm form) {


        adminService.checkNewRegion(form);

        return "redirect:/admin/course";
    }

    @GetMapping("/course-detail")
    public String courseDetail(@RequestParam(name = "no") int courseNo,
                               Model model) {
        // 1. 코스의 상세 정보를 가져온다.
        Course course = courseService.getCourseDetail(courseNo);

        // 3. Model 객체에 코스의 상세 정보를 저장한다.
        model.addAttribute("course", course);

        return "admin/course-admin-detail";
    }

    /*코스 목록*/
    @GetMapping("/course")
    public String course(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                         @RequestParam(name = "sort", required = false) String sort,
                         @RequestParam(name = "distance", required = false, defaultValue = "10") Double distance,
                         @RequestParam(name = "level", required = false) Integer level,
                         @RequestParam(name = "region", required = false) String region,
                         @RequestParam(name = "keyword", required = false) String keyword,
                         Model model) {
        // 1. 요청 파라미터 정보를 Map 객체에 담는다.
        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        if (StringUtils.hasText(sort)) {
            condition.put("sort", sort);
        }
        if (distance != null) {
            condition.put("distance", distance);
        }
        if (level != null) {
            condition.put("level", level);
        }
        if (StringUtils.hasText(keyword)) {
            condition.put("region", region);
            condition.put("keyword", keyword);
        }
        // 2. 검색에 해당하는 코스 목록을 가져온다.
        ListDto<Course> dto = courseService.getAllCourses(condition);

        // 3. Model 객체에 화면에 표시할 데이터를 저장해서 보낸다.
        model.addAttribute("courses", dto.getData());
        model.addAttribute("paging", dto.getPaging());

        return "admin/courselist";
    }

    @GetMapping("/user/preview")
    @ResponseBody
    public User preview(@RequestParam("no") int no) {
        User user = adminService.getUser(no);

        return user;
    }

    @GetMapping("/blacklist")
    public String blacklist() {
        return "admin/blacklist";
    }

    @GetMapping("/user")
    public String user(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
            @RequestParam(name = "opt", required = false) String opt,
            @RequestParam(name = "value", required = false) String value,
            Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);
        if (StringUtils.hasText(value)) {
            condition.put("opt", opt);
            condition.put("value", value);
        }

        // 검색조건을 전달해서 게시글 목록 조회
        ListDto<User> dto = adminService.getAllUsers(condition);
        // List<User>를 "users"로 모델에 저장
        model.addAttribute("users", dto.getData());
        // Pagination을 "paging"로 모델에 저장
        model.addAttribute("paging", dto.getPaging());

        return "admin/userlist";
    }

    @GetMapping("/register-editform")
    public String registerEditForm(@RequestParam("no") int no,
                                   @RequestParam(value = "colorNo", required = false) Integer colorNo,
                                   Model model) {

        Product product = adminService.getProductNo(no);

        List<Color> colors = adminService.getColorName(no);

        model.addAttribute("colors", colors);
        model.addAttribute("product", product);


        return "admin/product-edit-form";
    }

    @PostMapping("/register-editform")
    public String registerEditTitle(Product product,
                                    RedirectAttributes redirectAttributes) {
        try {
            adminService.getUpdateProduct(product); // 수정 처리
            redirectAttributes.addFlashAttribute("successMessage", "수정이 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "수정 중 오류가 발생했습니다.");
        }

        return "redirect:/admin/register-editform?no=" + product.getNo() + "&colorNo=" + product.getColorNum();
    }

    @GetMapping("/delete-size")
    public String getDeleteSize(@RequestParam("no") int no,
                                @RequestParam("colorNo") Integer colorNo,
                                Model model) {

        // 상품 정보 가져오기
        Product product = adminService.getProductNo(no);
        List<Color> colors = adminService.getColorName(no);
        Color color = adminService.getColorNo(colorNo);
        List<Size> sizes = adminService.getAllSizeByColorNo(colorNo);

        model.addAttribute("product", product);
        model.addAttribute("colors", colors);
        model.addAttribute("color", color);
        model.addAttribute("sizes", sizes);

        if (sizes == null || sizes.isEmpty()) {
            model.addAttribute("sizeMessage", "등록된 사이즈가 없습니다.");
            sizes = Collections.emptyList(); // 비어 있는 리스트 전달
        }

        return "admin/product-size-delete-form";
    }

    @PostMapping("/delete-size")
    public String deleteSize(@RequestParam("no") int no,
                             @RequestParam("colorNo") Integer colorNo,
                             @RequestParam("sizeNo") int sizeNo,
                             Model model) {

        adminService.getDeletedSize(sizeNo);

        return "redirect:/admin/delete-size?no=" + no + "&colorNo=" + colorNo;
    }

    @GetMapping("/register-size")
    public String registerSize(@RequestParam("no") int no,
                               @RequestParam("colorNo") Integer colorNo,
                               Model model) {

        // 상품 정보 가져오기
        Product product = adminService.getProductNo(no);
        List<Color> colors = adminService.getColorName(no);
        Color color = adminService.getColorNo(colorNo);
        List<Size> sizes = adminService.getAllSizeByColorNo(colorNo);

        if (sizes == null || sizes.isEmpty()) {
            model.addAttribute("sizeMessage", "등록된 사이즈가 없습니다.");
            sizes = Collections.emptyList(); // 비어 있는 리스트 전달
        }
        // 모델에 데이터 추가
        model.addAttribute("product", product);
        model.addAttribute("colors", colors);
        model.addAttribute("color", color);
        model.addAttribute("sizes", sizes);

        return "admin/product-size-register-form";
    }

    @PostMapping("/register-size")
    public String registerSize(@RequestParam("no") int no,
                               @RequestParam("colorNo") Integer colorNo,
                               @RequestParam("size") String size,
                               RedirectAttributes redirectAttributes) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("colorNo", colorNo);
        condition.put("size", size);

        try {

            adminService.getCheckSize(condition);
        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }


        return "redirect:/admin/register-size?no=" + no + "&colorNo=" + colorNo;


    }


    @GetMapping("/image-editform")
    public String getImageEditForm(@RequestParam("no") int no,
                                   @RequestParam("colorNo") Integer colorNo,
                                   Model model) {

        Product product = adminService.getProductNo(no);

        Color color = adminService.getColorNo(colorNo);

        List<Color> colors = adminService.getColorName(no);
        List<Image> images = adminService.getImageByColorNo(colorNo);

        model.addAttribute("product", product);
        model.addAttribute("color", color);
        model.addAttribute("colors", colors);
        model.addAttribute("images", images);

        return "admin/product-image-edit-form";
    }

    @PostMapping("/image-editform")
    public String imageEditForm(@RequestParam("no") int no,
                                @RequestParam("colorNo") Integer colorNo,
                                @RequestParam("imgNo") List<Integer> imgNoList,
                                @RequestParam("url") List<String> urlList) {

        List<Image> images = adminService.getImageByColorNo(colorNo);

        adminService.getEditUrl(imgNoList, urlList);

        return "redirect:/admin/product-detail?no=" + no + "&colorNo=" + colorNo;
    }

    @GetMapping("/image-changeThumb")
    public String getImageChangeForm(@RequestParam("no") int no,
                                     @RequestParam("colorNo") Integer colorNo,
                                     Model model) {

        Product product = adminService.getProductNo(no);

        List<Color> colors = adminService.getColorName(no);

        Color color = adminService.getColorNo(colorNo);

        List<Image> images = adminService.getImageByColorNo(colorNo);
        model.addAttribute("images", images);

        if (images.isEmpty()) {
            model.addAttribute("noImages", true); // 이미지가 없다는 플래그 추가
        } else {
            model.addAttribute("images", images);
        }

        model.addAttribute("color", color);
        model.addAttribute("colors", colors);
        model.addAttribute("product", product);

        return "admin/product-image-title-form";
    }

    @PostMapping("/image-changeThumb")
    public String imageChangeForm(@RequestParam("no") int no,
                                  @RequestParam("colorNo") Integer colorNo,
                                  @RequestParam("imgNo") Integer imgNo,
                                  @RequestParam("url") String url,
                                  Model model) {

        List<Image> images = adminService.getImageByColorNo(colorNo);

        model.addAttribute("images", images);

        adminService.getNullImageThumbyimgNo(imgNo);

        adminService.getThumbnailByNo(imgNo);

        return "redirect:/admin/product-detail?no=" + no + "&colorNo=" + colorNo;
    }

    @GetMapping("/register-image")
    public String getRegisterImage(@RequestParam("no") int no,
                                   Model model) {


        Product product = adminService.getProductNo(no);
        List<Color> colors = adminService.getColorName(no);

        model.addAttribute("colors", colors);
        model.addAttribute("product", product);

        return "admin/product-image-register-form";
    }

    @PostMapping("/register-image")
    public String registerImage(@ModelAttribute ColorThumbnailForm form,
                                @RequestParam("image[]") List<String> links,  // 이미지 URL 배열로 받기
                                RedirectAttributes redirectAttributes) {

        try {
            adminService.addThumb(form, links);
            redirectAttributes.addFlashAttribute("successMessage", "이미지가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미지 등록 중 오류가 발생했습니다.");
        }

        return "redirect:/admin/register-image?no=" + form.getProdNo() + "&colorNo=" + form.getColorNo();
    }

    @GetMapping("/register-color")
    public String getRegisterColor(@RequestParam("no") int no,
                                   Model model) {

        ProdDetailDto prodDetailDto = productService.getProductByNo(no);
        model.addAttribute("prodDetailDto", prodDetailDto);

        return "admin/product-color-register-form";
    }

    @PostMapping("/register-color")
    public String registerColor(@RequestParam(name="no", required = false) Integer no,
                                @RequestParam(name="name", required = false) String name,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (no == null || name == null || name.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "상품 번호와 색상은 필수 입력 값입니다.");
            return "redirect:/admin/register-color?no=" + no;
        }

        Map<String, Object> condition = new HashMap<>();
        condition.put("no", no);
        condition.put("name", name);

        model.addAttribute("condition", condition);
        adminService.addColor(condition);

        int colorNo = adminService.getColor(condition);

        redirectAttributes.addFlashAttribute("successMessage", "등록되었습니다.");

        return "redirect:/admin/register-color?no=" + condition.get("no") + "&colorNo=" + colorNo;
    }

    @GetMapping("/product-detail")
    public String productAdminDetail(@RequestParam("no") int no,
                                     @RequestParam("colorNo") int colorNo,
                                     Model model) {

        ProdDetailDto prodDetailDto = productService.getProductByNo(no);
        model.addAttribute("prodDetailDto", prodDetailDto);

        List<ColorProdImgDto> colorProdImgDto = productService.getProdImgByColorNo(no);
        model.addAttribute("colorProdImgDto", colorProdImgDto);

        SizeAmountDto sizeAmountDto = productService.getSizeAmountByColorNo(colorNo);
        model.addAttribute("sizeAmountDto", sizeAmountDto);

        ProdImagesDto prodImagesDto = productService.getProdImagesByColorNo(colorNo);
        model.addAttribute("prodImagesDto", prodImagesDto);

        Color color = adminService.getColorNo(colorNo);

        model.addAttribute("color", color);

        return "admin/product-admin-detail";
    }


    @GetMapping("/product-register-form")
    public String productRegisterForm() {

        return "admin/product-register-form";
    }

    @PostMapping("/product-register-form")
    public String productRegisterForm(ProductRegisterForm form, Model model) {

        adminService.addProduct(form);

        Category category = adminService.getCategory(form.getCategoryNo());

        return "redirect:/admin/product?topNo=" + category.getTopNo();
    }

    @GetMapping("/product-stock-detail")
    public String getProductStockDetail(@RequestParam("no") int no,
                                        @RequestParam("colorNo") Integer colorNo,
                                        @RequestParam(name = "colorName", required = false) String colorName,
                                        Model model) {

        Product product = adminService.getProductNo(no);
        List<Color> colors = adminService.getColorName(no);
        List<Size> sizes = adminService.getAllSizeByColorNo(colorNo);

        Map<String, Object> condition = new HashMap<>();
        condition.put("no", no);
        condition.put("colorName", colorName);

        List<Color> colorSize = adminService.getStockByColorNum(condition);


        if (colorSize == null || colorSize.isEmpty()) {
            model.addAttribute("colorSize", null);
            model.addAttribute("sizeMessage", "사이즈 정보가 없습니다.");

        } else {
            model.addAttribute("colorSize", colorSize);
            model.addAttribute("sizeMessage", null);


        }

        model.addAttribute("colorSize", colorSize);
        model.addAttribute("sizes", sizes);
        model.addAttribute("product", product);
        model.addAttribute("colors", colors);

        return "admin/product-stock-detail";
    }

    @PostMapping("/product-stock-detail")
    public String productStockDetail(@RequestParam("no") int no,
                                     @RequestParam(name = "colorNo") Integer colorNo,
                                     @RequestParam(name = "colorName", required = false) String colorName,
                                     @RequestParam("size") List<String> size,
                                     @RequestParam("amount") List<Integer> amount,
                                     Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("no", no);
        condition.put("colorName", colorName);

        for (int i = 0; i < size.size(); i++) {
            String currentSize = size.get(i);
            Integer currentAmount = amount.get(i);

            condition.put("size", currentSize);
            condition.put("amount", currentAmount);


            adminService.getInsertStock(condition);
        }

        return "redirect:/admin/product-detail?no=" + no + "&colorNo=" + colorNo;
    }
/*
    @GetMapping("/product-stock")
    public String getProductStock(@RequestParam(name = "topNo") int topNo,
                                  @RequestParam(name = "catNo", required = false, defaultValue = "0") int catNo,
                                  @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                  @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
                                  @RequestParam(name = "sort", required = false, defaultValue = "date") String sort,
                                  @RequestParam(name = "opt", required = false) String opt,
                                  @RequestParam(name = "value", required = false) String value,

                                  Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("topNo", topNo);
        if (catNo != 0) {
            condition.put("catNo", catNo);
        }

        condition.put("page", page);
        condition.put("rows", rows);
        condition.put("sort", sort);
        if(StringUtils.hasText(opt)) {
            condition.put("opt", opt);
            condition.put("value", value);
        }

        ListDto<ProdListDto> dto = adminService.getStockProduct(condition);
        model.addAttribute("topNo", topNo);
        model.addAttribute("catNo", catNo);
        model.addAttribute("products", dto.getData());
        model.addAttribute("paging", dto.getPaging());

        return "admin/product-stock";
    }
*/
    @PostMapping("/product-stock")
    public String productStock(@RequestParam("Y") String Y,
                               @RequestParam("no") int no,
                               @RequestParam("topNo") int topNo){

        Map<String, Object> condition = new HashMap<>();
        condition.put("Y", Y);
        condition.put("no", no);

        adminService.getDeletedProd(condition);

        return  "redirect:/admin/product-stock?topNo=" + topNo;
    }

    @PostMapping("/product-show")
    public String productShow(@RequestParam("no") int no,
                              @RequestParam("topNo") int topNo,
                              @RequestParam("show") String show) {


        Map<String, Object> condition = new HashMap<>();
        condition.put("no", no);
        condition.put("show", show);

        // 노출 상태 변경 처리
        adminService.updateProductShowStatus(condition);

        return "redirect:/admin/product-stock?topNo=" + topNo;
    }

//    @GetMapping("/product")
//    public String list(@RequestParam(name= "topNo") int topNo,
//                       @RequestParam(name = "catNo", required = false, defaultValue = "0") int catNo,
//                       @RequestParam(name = "page", required = false, defaultValue = "1") int page,
//                       @RequestParam(name = "rows", required = false, defaultValue = "6") int rows,
//                       @RequestParam(name = "sort" , required = false, defaultValue = "date") String sort,
//                       @RequestParam(name = "opt", required = false) String opt,
//                       @RequestParam(name = "value", required = false) String value,
//                       Model model) {
//
//        Map<String, Object> condition = new HashMap<>();
//        condition.put("topNo", topNo);
//        if(catNo != 0) {
//            condition.put("catNo", catNo);
//        }
//
//        condition.put("page", page);
//        condition.put("rows", rows);
//        condition.put("sort", sort);
//        if(StringUtils.hasText(opt)) {
//            condition.put("opt", opt);
//            condition.put("value", value);
//        }
//
//        ListDto<ProdListDto> dto = adminService.getStockProduct(condition);
//        model.addAttribute("topNo", topNo);
//        model.addAttribute("catNo", catNo);
//        model.addAttribute("products", dto.getData());
//        model.addAttribute("paging", dto.getPaging());
//
//        return "admin/productlist";
//    }

    @PostMapping("/updateDeliveryStatus")
    public String updateDeliveryStatus(@RequestParam(name="page", required = false,defaultValue ="1") int page,
                                       @RequestParam(name="rows", required = false,defaultValue ="10") int rows,
                                       @RequestParam(name="day", required = false) String day,
                                       @RequestParam("deliNo") int deliNo,
                                       @RequestParam("deliStatus") String deliStatus) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("deliNo", deliNo);
        condition.put("deliStatus", deliStatus);

        System.out.println("----------------condition" + condition);

        adminService.getUpdateDelivery(condition);


        return "redirect:/admin/order-delivery?page=" + page + "&rows" + rows + "&day=" + day;
    }

    @GetMapping("/order-delivery")
    public String order(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                        @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
                        @RequestParam(name = "day", required = false) String day,
                        @RequestParam(name = "sort", required = false, defaultValue = "latest") String sort,
                        @RequestParam(name = "opt", required = false) String opt,
                        @RequestParam(name = "keyword", required = false) String keyword,
                        @RequestParam(name = "value", required = false) String value,
                        Model model){

        if (day == null || day.isEmpty()) {
            // 현재 날짜로 기본 설정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            day = sdf.format(new Date());
        }

        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);
        condition.put("day", day);
        condition.put("sort", sort);

        if(StringUtils.hasText(opt)) {
            condition.put("opt", opt);
            condition.put("keyword", keyword);
            condition.put("value", value);
        }


        ListDto<orderDeliveryDto> dto = adminService.getOrderDelivery(condition);

        model.addAttribute("dto", dto.getData());
        model.addAttribute("paging", dto.getPaging());

        return "admin/order-delivery";
    }



    @GetMapping("/chart")
    public Map<String, Object> chart(@RequestParam(name = "day", required = false) String day,

                                     Model model) {

        if (day == null || day.isEmpty()) {
            // 현재 날짜로 기본 설정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            day = sdf.format(new Date());
        }

        Map<String, Object> conditions = adminService.getTotalSubject(day);

        model.addAttribute("conditions", conditions);

        return conditions;
    }

    @GetMapping("/p-settlement/preview")
    @ResponseBody
    public List<prevOrderProdDto> prodPreview(@RequestParam("orderNo") int orderNo
    ) {

        List<prevOrderProdDto> dtos = adminService.getOrderProdPrev(orderNo);


        return dtos;
    }

    @GetMapping("/p-settlement")
    public String pSettlement(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                              @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
                              @RequestParam(name = "sort", required = false) String sort,
                              @RequestParam(name = "opt", required = false, defaultValue = "all") String opt,
                              @RequestParam(name = "day", required = false) String day,
                              @RequestParam(name = "keyword", required = false, defaultValue = "all") String keyword,
                              @RequestParam(name = "value", required = false) String value,
                              Model model) {

        if (day == null || day.isEmpty()) {
            // 현재 날짜로 기본 설정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            day = sdf.format(new Date());
        }

            Map<String, Object> condition = new HashMap<>();
            condition.put("page", page);
            condition.put("rows", rows);
            condition.put("sort", sort);
            condition.put("opt", opt);

            if (StringUtils.hasText(day)) {
                condition.put("day", day);
            }

            if (StringUtils.hasText(value)) {
                condition.put("keyword", keyword);
                condition.put("value", value);
            }

            ListDto<OrderProductDto> dto = adminService.getOrderProduct(condition);

            int totalPriceSum = dto.getData().stream()
                    .mapToInt(OrderProductDto::getTotalPrice)
                    .distinct()   // 중복된 값 제거 (한 번만 합산)
                    .limit(1)     // 첫 번째 값만 선택
                    .sum();

            model.addAttribute("totalPriceSum", totalPriceSum);

            model.addAttribute("dto", dto.getData());
            model.addAttribute("paging", dto.getPaging());

            return "admin/p-settlement";
    }

    @GetMapping("/settlement")
    public String settlement(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                             @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
                             @RequestParam(name = "pType", required = false, defaultValue = "lesson") String pType,
                             @RequestParam(name = "day", required = false) String day,
                             @RequestParam(name = "sort", required = false, defaultValue ="latest") String sort,
                             @RequestParam(name = "opt", required = false, defaultValue = "all") String opt,
                             @RequestParam(name = "keyword", required = false) String keyword,
                             @RequestParam(name = "value", required = false) String value,
                             Model model) {

        if (day == null || day.isEmpty()) {
            // 현재 날짜로 기본 설정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            day = sdf.format(new Date());
        }


        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);
        condition.put("pType", pType);

        condition.put("sort", sort);
        condition.put("opt", opt);

        if (StringUtils.hasText(day)) {
            condition.put("day", day);
        }

        if (StringUtils.hasText(value)) {
            condition.put("keyword", keyword);
            condition.put("value", value);
        }

        ListDto<SettlementDto> dto = adminService.getSettleList(condition);

        int totalPriceSum = dto.getData().stream()
                .mapToInt(SettlementDto::getTotalPrice)
                .distinct()   // 중복된 값 제거 (한 번만 합산)
                .limit(1)     // 첫 번째 값만 선택
                .sum();

        model.addAttribute("totalPriceSum", totalPriceSum);
        model.addAttribute("dto", dto.getData());
        model.addAttribute("paging", dto.getPaging());

        return "admin/settlement";
    }

    @GetMapping("/qna")
    public String qna(@ModelAttribute RequestParamsDto dto ,Model model, @AuthenticationPrincipal LoginUser loginUser) {

//        // 검색 조건이 'status'일 때, keyword 값을 0, 1, 2로 변환
//        if ("status".equals(dto.getOpt()) && StringUtils.hasText(dto.getKeyword())) {
//            String status = dto.getKeyword();
//            // "대기", "완료", "삭제"를 0, 1, 2로 변환
//            if ("대기".equals(status)) {
//                condition.put("keyword", 0);
//            } else if ("완료".equals(status)) {
//                condition.put("keyword", 1);
//            } else if ("삭제".equals(status)) {
//                condition.put("keyword", 2);
//            }
//        }

        ListDto<QnaResponse> qnaDto = qnaService.getQnas(dto,loginUser.getNo());

        model.addAttribute("qnaList", qnaDto.getData());
        model.addAttribute("pagination", qnaDto.getPaging());

        return "admin/qnalist";
    }

    @GetMapping("/qna/{qnaNo}")
    public String qnaDetailPage(@PathVariable("qnaNo") int qnaNo, Model model){

        QnaResponse qnaResponse = qnaService.getQnaByQnaNo(qnaNo);

        model.addAttribute("qna",qnaResponse);

        return "admin/answer-form";
    }

    @PostMapping("/qna/answer")
    public String qnaAnswerPage(@ModelAttribute AnswerDTO answerDTO, @AuthenticationPrincipal LoginUser loginUser){

        qnaService.updateAnswer(answerDTO, loginUser.getNo());

        return "redirect:/admin/qna";
    }

    @PostMapping("/updateReport")
    public String updateReport(@RequestParam("reportNo") int reportNo,
                               @RequestParam("reportType") String reportType,
                               Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("reportNo", reportNo);
        condition.put("reportType", reportType);

        adminService.UpdateReport(condition);



        return "redirect:/admin/report";
    }

    @GetMapping("/report")
    public String report(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                         @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
                         @RequestParam(name = "sort", required = false, defaultValue = "latest") String sort,
                         @RequestParam(name = "opt", required = false) String opt,
                         @RequestParam(name = "value", required = false) String value,
                         @RequestParam(name = "keyword", required = false) String keyword,
                         Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);
        condition.put("sort", sort);

        if (StringUtils.hasText(value)) {
            condition.put("opt", opt);
            condition.put("keyword", keyword);
            condition.put("value", value);

        }

        ListDto<ReportDto> dto = adminService.getReport(condition);

        model.addAttribute("dto", dto.getData());
        model.addAttribute("paging", dto.getPaging());

        return "admin/reportlist";
    }

    @GetMapping("/community")
    public String community() {

        return "admin/community";
    }

    @GetMapping("/notice")
    public String notice(@RequestParam(name = "page", required = false, defaultValue = "1") int page
            , @RequestParam(name = "rows", required = false, defaultValue = "10") int rows
            , @RequestParam(name = "sort", required = false, defaultValue = "import") String sort
            , @RequestParam(name = "opt", required = false) String opt
            , @RequestParam(name = "keyword", required = false) String keyword
            , Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);
        condition.put("sort", sort);

        if (StringUtils.hasText(keyword)) {
            condition.put("opt", opt);
            condition.put("keyword", keyword);
        }

        ListDto<Notice> dto = noticeService.getNotices(condition);

        model.addAttribute("notices", dto.getData());
        model.addAttribute("paging", dto.getPaging());


        return "admin/notice";
    }

    @GetMapping("/marathon")
    public String marathon(@RequestParam(name = "page", required = false, defaultValue = "1") int page
            , @RequestParam(name = "rows", required = false, defaultValue = "6") int rows
            , @RequestParam(name = "opt", required = false) String opt
            , @RequestParam(name = "category", required = false) String category
            , @RequestParam(name = "keyword", required = false) String keyword
            , Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);

        // 카테고리 필터링 처리
        if (StringUtils.hasText(category)) {
            condition.put("category", category);
        }

        // 검색
        if (StringUtils.hasText(keyword)) {
            condition.put("opt", opt);
            condition.put("keyword", keyword);
        }

        ListDto<Marathon> dto = marathonService.getMarathons(condition);

        model.addAttribute("marathons", dto.getData());
        model.addAttribute("paging", dto.getPaging());
        model.addAttribute("now", new Date());

        return "admin/marathon";
    }
}
