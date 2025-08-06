package store.seub2hu2.community.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.community.dto.NoticeForm;
import store.seub2hu2.community.exception.CommunityException;
import store.seub2hu2.community.mapper.NoticeMapper;
import store.seub2hu2.community.mapper.UploadMapper;
import store.seub2hu2.community.vo.Notice;
import store.seub2hu2.community.vo.UploadFile;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.util.FileUtils;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.Pagination;
import store.seub2hu2.util.S3Service;

import java.util.List;
import java.util.Map;

@Service
public class NoticeService {

    @Value("${upload.directory.notice.files}")
    private String saveDirectory;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    //@Autowired
    //private S3Service s3Service;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private UploadMapper uploadMapper;

    public Notice addNewNotice(NoticeForm form) {

        Notice notice = new Notice();
        notice.setNo(form.getNo());
        notice.setFirst(form.isFirst());
        notice.setTitle(form.getTitle());
        notice.setContent(form.getContent());

        MultipartFile multipartFile = form.getUpfile();

        if (multipartFile != null && !multipartFile.isEmpty()) {
            String originalFilename = multipartFile.getOriginalFilename();
            String filename = System.currentTimeMillis() + originalFilename;
            //s3Service.uploadFile(multipartFile, bucketName, saveDirectory, filename);

            UploadFile uploadFile = new UploadFile();
            uploadFile.setOriginalName(originalFilename);
            uploadFile.setSaveName(filename);

            notice.setUploadFile(uploadFile);
        }

        int topNoticeCount = noticeMapper.getTopNoticeCnt();
        if (topNoticeCount >= 5) {
            noticeMapper.updateTopNoticeToNotFirst();
        }

        noticeMapper.insertNotice(notice);

        if (notice.getUploadFile() != null) {
            UploadFile uploadFile = notice.getUploadFile();
            uploadFile.setNo(notice.getNo());
            uploadFile.setSaveName(notice.getUploadFile().getSaveName());
            uploadFile.setOriginalName(notice.getUploadFile().getOriginalName());

            uploadMapper.insertNoticeFile(uploadFile);
        }

        return notice;
    }

    public ListDto<Notice> getNotices(Map<String, Object> condition) {
        int totalRows = noticeMapper.getTotalRowsForNotice(condition);

        int page = (Integer) condition.get("page");
        int rows = (Integer) condition.get("rows");
        Pagination pagination = new Pagination(page, totalRows, rows);

        condition.put("begin", pagination.getBegin());
        condition.put("end", pagination.getEnd());

        List<Notice> notices = noticeMapper.getNotices(condition);
        ListDto<Notice> dto = new ListDto<>(notices, pagination);

        return dto;
    }

    public Notice getNoticeDetail(int noticeNo) {
        Notice notice = noticeMapper.getNoticeByNo(noticeNo);

        if (notice.getUploadFile() != null) {
            UploadFile uploadFile = uploadMapper.getFileByNoticeNo(noticeNo);
            notice.setUploadFile(uploadFile);
        }

        return notice;
    }

    public void updateNoticeViewCnt(int noticeNo) {
        Notice notice = noticeMapper.getNoticeByNo(noticeNo);
        notice.setViewCnt(notice.getViewCnt() + 1);
        noticeMapper.updateNoticeCnt(notice);
    }

    public Notice updateNotice(NoticeForm form) {
        Notice savedNotice = noticeMapper.getNoticeByNo(form.getNo());
        savedNotice.setFirst(form.isFirst());
        savedNotice.setTitle(form.getTitle());
        savedNotice.setContent(form.getContent());
        savedNotice.setDeleted("N");

        MultipartFile multipartFile = form.getUpfile();

        // 수정할 첨부파일이 있으면,
        if (multipartFile != null && !multipartFile.isEmpty()) {
            // 기존 파일 정보 조회
            UploadFile prevFile = uploadMapper.getFileByNoticeNo(savedNotice.getNo());
            // 기존 파일이 있으면 기존 파일 삭제
            if (prevFile != null) {
                prevFile.setDeleted("Y");
                uploadMapper.updateNoticeFile(prevFile);
            }

            String originalFilename = multipartFile.getOriginalFilename();
            String filename = System.currentTimeMillis() + originalFilename;
            //s3Service.uploadFile(multipartFile, bucketName, saveDirectory, filename);

            UploadFile uploadFile = new UploadFile();
            uploadFile.setOriginalName(originalFilename);
            uploadFile.setSaveName(filename);
            uploadFile.setNo(savedNotice.getNo());
            savedNotice.setUploadFile(uploadFile);

            uploadMapper.insertNoticeFile(uploadFile);
        }

        int topNoticeCount = noticeMapper.getTopNoticeCnt();
        if (topNoticeCount >= 5) {
            noticeMapper.updateTopNoticeToNotFirst();
        }

        noticeMapper.updateNotice(savedNotice);

        return savedNotice;
    }

    public void deleteNotice(int noticeNo) {
        Notice notice = noticeMapper.getNoticeByNo(noticeNo);
        notice.setDeleted("Y");
        notice.setFirst(false);
        notice.setTitle(notice.getTitle());
        notice.setContent(notice.getContent());
        notice.setUploadFile(notice.getUploadFile());

        noticeMapper.updateNotice(notice);
    }

    public void deleteNoticeFile(int noticeNo, int fileNo){
        UploadFile uploadFile = uploadMapper.getFileByNoticeNo(noticeNo);
        uploadFile.setNo(fileNo);
        uploadFile.setDeleted("Y");

        uploadMapper.updateNoticeFile(uploadFile);
    }

    public void updateTopFive(NoticeForm form) {

    }
}
