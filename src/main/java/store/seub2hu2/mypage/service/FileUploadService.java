package store.seub2hu2.mypage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.mypage.mapper.PostMapper;
import store.seub2hu2.user.mapper.UserMapper;
import store.seub2hu2.user.vo.UserImage;
import store.seub2hu2.util.S3Service;

import java.io.IOException;
import java.util.*;

@Service
public class FileUploadService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${upload.directory.userImage}")
    private String saveDirectory;

    //@Autowired
    //private S3Service s3Service;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private UserMapper userMapper;

    public Map<String, Object> saveFile(List<MultipartFile> files, int postNo, String thumb) {
        List<Map<String, Object>> images = new ArrayList<>();
        String thumbnailBase64 = null;

        // 썸네일 처리 (base64로 전달된 경우)
        if (thumb != null && !thumb.isEmpty()) {
            thumbnailBase64 = thumb;  // base64 문자열을 썸네일로 저장
        }

        // 일반 이미지 파일을 base64로 변환하여 저장
        for (MultipartFile file : files) {
            String base64Image = convertFileToBase64(file);  // 파일을 base64로 변환

            // 이미지를 DB에 저장할 때 base64 문자열로 저장
            Map<String, Object> param = new HashMap<>();
            param.put("imageUrl", base64Image);  // base64 이미지 저장
            param.put("postNo", postNo);        // `postNo`를 이미지에 추가
            images.add(param);
        }



        // 이미지 정보 DB에 삽입
        postMapper.insertPostImages(images);

        // 반환할 응답 정보 준비
        Map<String, Object> response = new HashMap<>();
        response.put("thumbnailBase64", thumbnailBase64);  // 썸네일 base64 반환
        return response;
    }

    public String userImageUpload(MultipartFile file, int userNo){

        UserImage userImage = new UserImage();

        if(file != null){
            String originalFilename = file.getOriginalFilename();
            String filename = System.currentTimeMillis() + originalFilename;

            //s3Service.uploadFile(file, bucketName, saveDirectory, filename);

            userImage.setUserNo(userNo);
            userImage.setImgName(filename);

        }

        //1. 기존 isprimary 값을 n으로 변경
        userMapper.updatePrimaryToN(userNo);

        //2. 새 이미지를 INSERT하면서 isprimary값을 Y로 설정
        userMapper.insertUserImage(userImage);

        return userImage.getImgName();
    }

    // 파일을 base64로 변환하는 메소드 (MIME 타입 포함)
    private String convertFileToBase64(MultipartFile file) {
        try {
            // 파일의 MIME 타입을 추출
            String mimeType = file.getContentType();  // 예: image/png, image/jpeg 등
            byte[] fileBytes = file.getBytes();

            // "data:image/png;base64," 형태로 변환하여 반환
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("파일을 base64로 변환하는 중 오류가 발생했습니다.", e);
        }
    }

}