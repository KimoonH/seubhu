package store.seub2hu2.message.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import store.seub2hu2.community.vo.Board;
import store.seub2hu2.message.dto.MessageForm;
import store.seub2hu2.message.dto.MessageReceived;
import store.seub2hu2.message.service.MessageService;
import store.seub2hu2.message.vo.Message;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.user.service.UserService;
import store.seub2hu2.user.vo.User;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.S3Service;

import java.io.File;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/message")
public class MessageController {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private String folder = "resources/messages";

    //@Autowired
    //private S3Service s3Service;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    // 받은 메시지 목록 조회
    @GetMapping("/list")
    public String receivedList(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
            @RequestParam(name = "sort", required = false, defaultValue = "desc") String sort,
            @RequestParam(name = "opt", required = false) String opt,
            @RequestParam(name = "keyword", required = false) String keyword,
            @AuthenticationPrincipal LoginUser loginUser,
            Model model) {

        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);
        condition.put("sort", sort);
        condition.put("userNo", loginUser.getNo());  // 로그인된 사용자 번호


        if (StringUtils.hasText(keyword)) {
            condition.put("opt", opt);
            condition.put("keyword", keyword);
        }

        // 받은 메시지 목록 조회
        ListDto<MessageReceived> dto = messageService.getReceivedMessages(condition);

        // 모델에 데이터 추가
        model.addAttribute("messages", dto.getData());
        model.addAttribute("paging", dto.getPaging());
        model.addAttribute("condition", condition); // 검색 조건도 모델에 포함 (필요 시 사용)


        return "message/message-received-list";
    }

    // 보낸 메시지 목록 조회
    @GetMapping("/sent")
    public String sentList(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "rows", required = false, defaultValue = "10") int rows,
            @RequestParam(name = "sort", required = false, defaultValue = "desc") String sort, // 기본값 변경
            @RequestParam(name = "opt", required = false) String opt,
            @RequestParam(name = "keyword", required = false) String keyword,
            @AuthenticationPrincipal LoginUser loginUser,
            Model model) {

        // 검색 조건 수집
        Map<String, Object> condition = new HashMap<>();
        condition.put("page", page);
        condition.put("rows", rows);
        condition.put("sort", sort);
        condition.put("userNo", loginUser.getNo()); // 로그인된 사용자 번호

        // 검색 조건 추가 (유효성 검증 포함)
        if (StringUtils.hasText(keyword)) {
            condition.put("opt", opt);
            condition.put("keyword", keyword);
        }

        // 메시지 목록 조회
        ListDto<MessageReceived> dto = messageService.getSentMessages(condition);

        // 모델에 데이터 추가
        model.addAttribute("messages", dto.getData());
        model.addAttribute("paging", dto.getPaging());
        model.addAttribute("condition", condition); // 검색 조건도 모델에 포함 (필요 시 사용)

        return "message/message-sent-list";
    }

    @GetMapping("/detail")
    public String detail(
            @RequestParam("messageNo") int messageNo,
            @AuthenticationPrincipal LoginUser loginUser,
            Model model) {
        int userNo = loginUser.getNo();  // 로그인된 사용자 번호

        // 메시지 상세 조회
        Message message = messageService.getMessageDetail(messageNo);

        model.addAttribute("message", message);
        model.addAttribute("userNo", userNo);  // userNo를 model에 추가

        return "message/message-detail";  // JSP 경로
    }




    // 쪽지 작성 폼 화면 반환
    @GetMapping("/add")
    public String showAddMessageForm(Model model, @AuthenticationPrincipal LoginUser loginUser) {
        // 작성자 닉네임 설정
        String senderNickname = loginUser.getNickname(); // LoginUser 객체에서 닉네임 가져오기

        // Model에 데이터 추가
        model.addAttribute("sender", senderNickname); // JSP에서 ${sender}로 사용 가능

        // 초기 MessageForm 객체 추가 (필요 시)
        model.addAttribute("messageForm", new MessageForm());

        return "message/message-send-form"; // JSP 파일 경로
    }

    //메세지 보내기
    @PostMapping("/add")
    public String addMessage(MessageForm form, @AuthenticationPrincipal LoginUser loginUser) {
        messageService.sendMessage(form, loginUser.getNo());
        return "redirect:/message/list"; // 메시지 보내기 후 받은 메시지 목록으로 리다이렉트
    }


    // 개별 삭제
    @GetMapping("/delete")
    public String deleteMessage(@RequestParam("messageNo") int messageNo) {
        messageService.deleteMessage(messageNo);
        return "redirect:/message/sent";
    }

    @GetMapping("/markAsRead")
    public String markAsRead(@RequestParam("messageNo") int messageNo, @AuthenticationPrincipal LoginUser loginUser) {
        int userNo = loginUser.getNo();  // 로그인된 사용자 번호

        // 읽음 처리
        messageService.markAsRead(messageNo, userNo);

        // 메시지 목록으로 리디렉션
        return "redirect:/message/detail?messageNo=" + messageNo;
    }



    @GetMapping("/deleteMultiple")
    public String deleteMultiple(@RequestParam("messageNo") List<Integer> messageNo, @AuthenticationPrincipal LoginUser loginUser) {
        messageService.deleteMessageRcvs(messageNo, loginUser.getNo());
        return "redirect:/message/list"; // 메시지 리스트 페이지로 리다이렉트
    }

    @GetMapping("/markMultipleAsRead")
    public String markMultipleAsRead(@RequestParam("messageNo") List<Integer> messageNos, @AuthenticationPrincipal LoginUser loginUser ) {
        messageService.updateReadMarkMessageRcvs(messageNos, loginUser.getNo());
        return "redirect:/message/list";
    }

/*
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadfile(@RequestParam("no") int no) throws Exception {
        // 게시글 정보 조회
        Message message = messageService.getMessageDetail(no);

        // 파일명을 조회
        String filename = message.getMessageFile().getSavedName();
        String originalFilename = message.getMessageFile().getOriginalName();

        // 파일명을 UTF-8로 인코딩 (파일명에 한글이 있을 수 있기 때문에)
        String encodedFilename = URLEncoder.encode(originalFilename, "UTF-8").replaceAll("\\+", "%20");

        ByteArrayResource resource = s3Service.downloadFile(bucketName, folder, filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)  // 파일명 인코딩 처리
                .body(resource);
    }
*/
}
