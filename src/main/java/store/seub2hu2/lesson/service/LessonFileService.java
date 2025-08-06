package store.seub2hu2.lesson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import store.seub2hu2.lesson.mapper.LessonFileMapper;
import store.seub2hu2.lesson.mapper.LessonMapper;
import store.seub2hu2.lesson.vo.LessonFile;
import store.seub2hu2.util.FileUtils;
import store.seub2hu2.util.S3Service;
import store.seub2hu2.util.WebContentFileUtils;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonFileService {

    @Value("${upload.directory.lesson}")
    private String saveDirectory;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    //private final S3Service s3Service;
    private final LessonFileMapper lessonFileMapper;

    public void saveLessonImages(Integer lessonNo, MultipartFile thumbnail, MultipartFile mainImage) {

        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                // Base64 인코딩
                String thumbnailBase64 = Base64.getEncoder().encodeToString(thumbnail.getBytes());

                // 저장할 파일명 생성
                String thumbnailFileName = UUID.randomUUID() + thumbnail.getOriginalFilename();
                //s3Service.uploadFile(thumbnail, bucketName, saveDirectory, thumbnailFileName);

                // LessonFile 객체 생성 (Base64 데이터를 저장소나 DB에 저장 가능)
                LessonFile thumbnailFile = new LessonFile(
                        lessonNo, thumbnailFileName, "THUMBNAIL", saveDirectory
                );
                thumbnailFile.setBase64Data(thumbnailBase64); // DB에 저장하고 싶다면 LessonFile 엔티티에 추가
                lessonFileMapper.insertLessonFile(thumbnailFile);

            } catch (IOException e) {
                throw new RuntimeException("Error while processing thumbnail file", e);
            }
        }

        if (mainImage != null && !mainImage.isEmpty()) {
            try {
                // Base64 인코딩
                String mainImageBase64 = Base64.getEncoder().encodeToString(mainImage.getBytes());

                // 저장할 파일명 생성
                String mainImageFileName = UUID.randomUUID() + mainImage.getOriginalFilename();
                //s3Service.uploadFile(mainImage, bucketName, saveDirectory, mainImageFileName);

                // LessonFile 객체 생성 (Base64 데이터를 저장소나 DB에 저장 가능)
                LessonFile mainImageFile = new LessonFile(
                        lessonNo, mainImageFileName, "MAIN_IMAGE", saveDirectory
                );
                mainImageFile.setBase64Data(mainImageBase64); // DB에 저장하고 싶다면 LessonFile 엔티티에 추가
                lessonFileMapper.insertLessonFile(mainImageFile);

            } catch (IOException e) {
                throw new RuntimeException("Error while processing main image file", e);
            }
        }
    }

    // lessonNo에 맞는 이미지 파일 정보 가져오기
    public Map<String, String> getImagesByLessonNo(int lessonNo) {
        List<LessonFile> lessonFiles = lessonFileMapper.getImagesByLessonNo(lessonNo);

        // 썸네일과 본문 이미지 경로를 Map으로 구분하여 반환
        Map<String, String> images = new HashMap<>();
        for (LessonFile file : lessonFiles) {
            if ("THUMBNAIL".equals(file.getFileType())) {
                images.put("THUMBNAIL", file.getFileName());
                log.info("THUMBNAIL = {}", file.getFileName());
            } else if ("MAIN_IMAGE".equals(file.getFileType())) {
                images.put("MAIN_IMAGE", file.getFileName());
                log.info("MAIN_IMAGE = {}", file.getFileName());
            }
        }

        log.info("썸네일 = {}", images.get("THUMBNAIL"));
        log.info("본문 = {}", images.get("MAIN_IMAGE"));

        return images;
    }

    public void updateLessonImages(Integer lessonNo, MultipartFile thumbnail, MultipartFile mainImage) {
        System.out.println("saveLessonImages lessonNo: " + lessonNo);

        log.info("updateLessonImages mainImage: " + mainImage.getOriginalFilename());

        if (thumbnail == null && thumbnail.isEmpty()) {
            try {
                // Base64 인코딩
                String thumbnailBase64 = Base64.getEncoder().encodeToString(thumbnail.getBytes());

                // 저장할 파일명 생성
                String thumbnailFileName = UUID.randomUUID() + thumbnail.getOriginalFilename();
                //s3Service.uploadFile(thumbnail, bucketName, saveDirectory, thumbnailFileName);

                // LessonFile 객체 생성 (Base64 데이터를 저장소나 DB에 저장 가능)
                LessonFile thumbnailFile = new LessonFile(
                        lessonNo, thumbnailFileName, "THUMBNAIL", saveDirectory
                );
                thumbnailFile.setBase64Data(thumbnailBase64); // DB에 저장하고 싶다면 LessonFile 엔티티에 추가
                lessonFileMapper.insertLessonFile(thumbnailFile);

            } catch (IOException e) {
                throw new RuntimeException("Error while processing thumbnail file", e);
            }
        }

        if (mainImage == null && mainImage.isEmpty()) {
            try {
                // Base64 인코딩
                String mainImageBase64 = Base64.getEncoder().encodeToString(mainImage.getBytes());

                // 저장할 파일명 생성
                String mainImageFileName = UUID.randomUUID() + mainImage.getOriginalFilename();
                //s3Service.uploadFile(thumbnail, bucketName, saveDirectory, mainImageFileName);

                // LessonFile 객체 생성 (Base64 데이터를 저장소나 DB에 저장 가능)
                LessonFile mainImageFile = new LessonFile(
                        lessonNo, mainImageFileName, "MAIN_IMAGE", saveDirectory
                );
                mainImageFile.setBase64Data(mainImageBase64); // DB에 저장하고 싶다면 LessonFile 엔티티에 추가
                lessonFileMapper.insertLessonFile(mainImageFile);

            } catch (IOException e) {
                throw new RuntimeException("Error while processing main image file", e);
            }
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            String thumbnailFileName = UUID.randomUUID() + thumbnail.getOriginalFilename();
            //s3Service.uploadFile(thumbnail, bucketName, saveDirectory, thumbnailFileName);
            LessonFile thumbnailFile = new LessonFile(
                    lessonNo, thumbnailFileName, "THUMBNAIL", saveDirectory
            );
            lessonFileMapper.updateLessonFile(thumbnailFile);
        }

        if (mainImage != null && !mainImage.isEmpty()) {
            String mainImageFileName = UUID.randomUUID() + mainImage.getOriginalFilename();
            //s3Service.uploadFile(thumbnail, bucketName, saveDirectory, mainImageFileName);
            LessonFile mainImageFile = new LessonFile(
                    lessonNo, mainImageFileName, "MAIN_IMAGE", saveDirectory
            );
            lessonFileMapper.updateLessonFile(mainImageFile);
        }

    }

}
