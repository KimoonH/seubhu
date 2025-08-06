package store.seub2hu2.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.message.dto.MessageForm;
import store.seub2hu2.message.dto.MessageReceived;
import store.seub2hu2.message.mapper.MessageMapper;
import store.seub2hu2.message.vo.Message;
import store.seub2hu2.message.vo.MessageFile;
import store.seub2hu2.user.mapper.UserMapper;
import store.seub2hu2.user.vo.User;
import store.seub2hu2.util.FileUtils;
import store.seub2hu2.util.ListDto;
import store.seub2hu2.util.Pagination;
import store.seub2hu2.util.S3Service;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private String folder = "resources/messages";

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${upload.directory.message}")
    private String saveDirectory;

    //@Autowired
    //private S3Service s3Service;



    public void sendMessage(MessageForm form, int userNo) {
        // 메시지 객체 생성
        Message message = new Message();
        message.setUserNo(userNo);
        message.setTitle(form.getTitle());
        message.setContent(form.getContent());

        // 메시지 삽입 후 messageNo 반환
        messageMapper.insertMessage(message); // 메시지 삽입 후 자동으로 messageNo가 할당됩니다.

        // 첨부파일 처리
        MultipartFile multipartFile = form.getFile();
        if (!multipartFile.isEmpty()) {
            String originalFilename = multipartFile.getOriginalFilename();
            String filename = System.currentTimeMillis() + originalFilename;
            long fileSize = multipartFile.getSize();  // 파일 사이즈

            //s3Service.uploadFile(multipartFile, bucketName, folder, filename);
            // MessageFile 객체 생성
            MessageFile messageFile = new MessageFile();
            messageFile.setOriginalName(originalFilename);
            messageFile.setSavedName(filename);
            messageFile.setSize(fileSize);
            messageFile.setType(multipartFile.getContentType());  // 파일 MIME 타입
            messageFile.setMessageNo(message.getMessageNo());  // 메시지 번호 설정

            // 첨부파일을 MESSAGE_FILES 테이블에 삽입
            messageMapper.insertMessageFile(messageFile);  // insertMessageFile 호출
        }

        // 수신자 처리
        String[] nicknames = form.getReceivers().split(",");
        for (String nickname : nicknames) {
            User user = userMapper.getUserByNickname(nickname.trim());
            if (user != null) {
                // MESSAGE_RECEIVERS 테이블에 수신자 정보 저장
                messageMapper.insertMessageReceiver(message.getMessageNo(), user.getNo());
            }
        }
    }


    private ListDto<MessageReceived> getMessages(Map<String, Object> condition, boolean isReceived) {
        // 검색 조건에 맞는 데이터 전체 갯수 조회
        int totalRows = isReceived
            ? messageMapper.getMessageRcvTotalRows(condition)
                : messageMapper.getMessageTotalRows(condition);
        // pagination 객체 생성
        int page = (Integer) condition.getOrDefault("page", 1);
        int rows = (Integer) condition.getOrDefault("rows", 10);
        Pagination pagination = new Pagination(page, totalRows, rows);

        // 데이터 검색��위를 조회해서 Map에 저장
        condition.put("offset", pagination.getOffset());
        condition.put("rows", pagination.getRowsPerPage());

        // 받은 메시지 또는 ����� 메시지 조회
        List<MessageReceived> messages = isReceived
               ? messageMapper.getReceivedMessages(condition)
                : messageMapper.getSentMessages(condition);

        // 각 메시지별 파일 존재 여부 확인 후 설정
        for (MessageReceived message : messages) {
            boolean hasFile = messageMapper.hasFiles(message.getMessageNo()); // 파일 여부 확인
            message.setHasFile(hasFile); // 설정
        }

        // 반환할 DTO 생성
        return new ListDto<>(messages, pagination);
    }


    // 받은 메시지 목록 조회
    public ListDto<MessageReceived> getReceivedMessages(Map<String, Object> condition) {
        return getMessages(condition, true); // 받은 메시지 조회
    }

    // 보낸 메시지 목록 조회
    public ListDto<MessageReceived> getSentMessages(Map<String, Object> condition) {
        return getMessages(condition, false); // 보낸 메시지 조회
    }


    // 메시지 상세 조회
    public Message getMessageDetail(int messageNo) {
        return messageMapper.getMessageDetailByNo(messageNo);
    }


    // 단일 메시지 읽음 처리
    public void markAsRead(int messageNo, int userNo) {
        // 읽음 상태 업데이트
        messageMapper.updateReadMarkMessageRcv(messageNo, userNo);
        messageMapper.updateReadMarkMessage(messageNo);
    }


    // 단일 메시지 삭제
    public void deleteMessage ( int messageNo){

        messageMapper.deleteMessage(messageNo);
    }

    // 다중 메시지 삭제
    public void deleteMessageRcvs (List < Integer > messageNos, int userNo) {
        for (int messageNo : messageNos) {
            messageMapper.deleteMessageRcv(messageNo, userNo);
        }
    }


    // 다중 메시지 읽음 처리
    public void updateReadMarkMessageRcvs (List < Integer > messageNos, int userNo) {
        for (int messageNo : messageNos) {
            messageMapper.updateReadMarkMessageRcv(messageNo, userNo);
        }

    }



}
