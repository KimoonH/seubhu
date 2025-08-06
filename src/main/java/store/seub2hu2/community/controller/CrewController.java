package store.seub2hu2.community.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import store.seub2hu2.community.dto.CrewForm;
import store.seub2hu2.community.dto.ReplyForm;
import store.seub2hu2.community.dto.ReportForm;
import store.seub2hu2.community.service.CrewService;
import store.seub2hu2.community.service.ReplyService;
import store.seub2hu2.community.service.ReportService;
import store.seub2hu2.community.vo.Crew;
import store.seub2hu2.community.vo.CrewMember;
import store.seub2hu2.community.vo.Reply;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.S3Service;

import java.io.File;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/community/crew")
public class CrewController {

    @Value("${upload.directory.crew.images}")
    private String imageSaveDirectory;

    @Value("${upload.directory.crew.files}")
    private String fileSaveDirectory;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    //@Autowired
    //private S3Service s3Service;

    @Autowired
    public CrewService crewService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReplyService replyService;

    @GetMapping("/main")
    public String list(@RequestParam(name = "page", required = false, defaultValue = "1") int page
            , @RequestParam(name = "rows", required = false, defaultValue = "6") int rows
            , @RequestParam(name = "category", required = false) String category
            , @RequestParam(name = "opt", required = false) String opt
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

        ListDto<Crew> dto = crewService.getCrews(condition);

        model.addAttribute("crews", dto.getData());
        model.addAttribute("paging", dto.getPaging());

        return "community/crew/main";
    }

    @GetMapping("/form")
    public String form() {
        return "community/crew/form";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam("no") int crewNo
            , @AuthenticationPrincipal LoginUser loginUser
            , Model model) {
        Crew crew = crewService.getCrewDetail(crewNo);

        List<Reply> replyList = replyService.getReplies(crewNo);
        crew.setReply(replyList);
        int replyCnt = replyService.getReplyCnt(crewNo);

        int memberCnt = crewService.getEnterMemberCnt(crewNo);

        model.addAttribute("crew", crew);
        model.addAttribute("replies", replyList);
        model.addAttribute("replyCnt", replyCnt);
        model.addAttribute("memberCnt", memberCnt);

        if (loginUser != null) {
            for (Reply reply : replyList) {
                int replyResult = replyService.getCheckLike(reply.getNo(), "crewReply", loginUser);
                model.addAttribute("replyLiked", replyResult);

                Reply prev = replyService.getReplyDetail(reply.getNo());
                model.addAttribute("prev", prev);


            }
            boolean isExists = crewService.isExistCrewMember(crewNo, loginUser);
            model.addAttribute("isExists", isExists);
        }

        if (memberCnt < 5){
            crewService.updateCrewCondition(crewNo, "Y");
        } else {
            crewService.updateCrewCondition(crewNo, "N");
        }

        return "community/crew/detail";
    }

    @GetMapping("/hit")
    public String hit(@RequestParam("no") int crewNo){
        crewService.updateCrewViewCnt(crewNo);
        return "redirect:detail?no=" + crewNo;
    }

    @PostMapping("/register")
    @ResponseBody
    public Crew register(CrewForm form
            , @AuthenticationPrincipal LoginUser loginUser) {

        Crew crew = crewService.addNewCrew(form, loginUser);
        return crew;
    }

    @GetMapping("/modify")
    public String modifyForm(@RequestParam("no") int crewNo
            , @AuthenticationPrincipal LoginUser loginUser
            , Model model) {
        Crew crew = crewService.getCrewDetail(crewNo);
        model.addAttribute("crew", crew);

        return "community/crew/modify";
    }

    @PostMapping("/modify")
    @ResponseBody
    public Crew update(CrewForm form){

        Crew crew = crewService.updateCrew(form);
        return crew;
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("no") int crewNo){
        CrewForm form = new CrewForm();
        form.setNo(crewNo);
        crewService.deleteCrew(crewNo);

        return "redirect:main";
    }

    @GetMapping("/delete-file")
    public String deleteFile(@RequestParam("no") int crewNo
            , @RequestParam("fileNo") int fileNo){

        crewService.deleteCrewFile(fileNo);
        return "redirect:modify?no=" + crewNo;
    }

    @GetMapping("/delete-thumbnail")
    public String deleteThumbnail(@RequestParam("no") int crewNo
            , @RequestParam("thumbnailNo") int thumbnailNo){

        crewService.deleteCrewFile(thumbnailNo);
        return "redirect:modify?no=" + crewNo;
    }
/*
    @GetMapping("/filedown")
    public ResponseEntity<ByteArrayResource> download(@RequestParam("no") int crewNo) {

        Crew crew = crewService.getCrewDetail(crewNo);

        try {
            String originalFileName = crew.getUploadFile().getSaveName();
            String savedFilename = crew.getUploadFile().getSaveName();

            //ByteArrayResource byteArrayResource = s3Service.downloadFile(bucketName, fileSaveDirectory, savedFilename);

            String encodedFileName = URLEncoder.encode(savedFilename.substring(13), "UTF-8");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(byteArrayResource.contentLength())
                    .body(byteArrayResource);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
*/

    @GetMapping("/login")
    public String login(){
        return "redirect:../user/login";
    }

    @PostMapping("/add-reply")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public Reply addReply(ReplyForm form
            , @AuthenticationPrincipal LoginUser loginUser) {

        Reply reply = replyService.addNewReply(form, loginUser);

        return reply;
    }

    @PostMapping("/add-comment")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public Reply addComment(ReplyForm form
            , @AuthenticationPrincipal LoginUser loginUser){

        Reply reply = replyService.addNewComment(form, loginUser);
        return reply;
    }

    @PostMapping("/modify-reply")
    @PreAuthorize("isAuthenticated()")
    public String modifyReply(ReplyForm form
            , @AuthenticationPrincipal LoginUser loginUser){

//        try{
        Reply reply = replyService.updateReply(form, loginUser);
//            return reply;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
        return "redirect:detail?no=" + reply.getTypeNo();
    }

    @GetMapping("/delete-reply")
    @PreAuthorize("isAuthenticated()")
    public String deleteReply(@RequestParam("rno") int replyNo,
                              @RequestParam("no") int crewNo){

        ReplyForm form = new ReplyForm();
        form.setNo(replyNo);
        form.setCrewNo(crewNo);
        replyService.deleteReply(replyNo);

        return "redirect:detail?no=" + crewNo;
    }

    @PostMapping("/update-reply-like")
    @PreAuthorize("isAuthenticated()")
    public String updateReplyLke(@RequestParam("no") int crewNo
            , @RequestParam("rno") int replyNo
            , @AuthenticationPrincipal LoginUser loginUser){

        replyService.updateReplyLike(replyNo, "crewReply", loginUser);
        return "redirect:detail?no=" + crewNo;
    }

    @PostMapping("/delete-reply-like")
    public String updateReplyUnlike(@RequestParam("no") int crewNo
            , @RequestParam("rno") int replyNo
            , @AuthenticationPrincipal LoginUser loginUser){

        replyService.deleteReplyLike(replyNo, "crewReply", loginUser);
        return "redirect:detail?no=" + crewNo;
    }

    @PostMapping("/report-crew")
    @PreAuthorize("isAuthenticated()")
    public String reportCrew(ReportForm form
            , @AuthenticationPrincipal LoginUser loginUser){
        boolean isReported = reportService.isReported(form.getType(), form.getNo(), loginUser);

        if (!isReported){
            reportService.registerReport(form, loginUser);
        }

        return "redirect:detail?no=" + form.getNo();
    }

    @GetMapping("report-reply")
    @PreAuthorize("isAuthenticated()")
    public String reportReply(ReportForm form
            , @AuthenticationPrincipal LoginUser loginUser){
        boolean isReported = reportService.isReported(form.getType(), form.getNo(), loginUser);

        if (!isReported){
            reportService.registerReport(form, loginUser);
        }

        Reply reply = replyService.getReplyDetail(form.getNo());
        return "redirect:detail?no=" + reply.getTypeNo();
    }

    @GetMapping("/enter-crew")
    public String enterCrew(@RequestParam("no") int crewNo
            , @AuthenticationPrincipal LoginUser loginUser){
        crewService.enterCrew(crewNo, loginUser);
        return "redirect:detail?no=" + crewNo;
    }

    @GetMapping("/leave-crew")
    public String leaveCrew(@RequestParam("no") int crewNo
            , @AuthenticationPrincipal LoginUser loginUser){
        crewService.leaveCrew(crewNo, loginUser);
        return "redirect:detail?no=" + crewNo;
    }

    @GetMapping("report-check")
    @ResponseBody
    public String reportCheck(@RequestParam("type") String type
            , @RequestParam("no") int no
            , @AuthenticationPrincipal LoginUser loginUser){

        boolean isReported = reportService.isReported(type, no, loginUser);

        return isReported ? "yes" : "no";
    }
}