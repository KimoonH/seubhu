package store.seub2hu2.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// S3 임시 비활성화
// @Service
@RequiredArgsConstructor
public class S3Service {
	
	private final S3Client s3Client;
	
	/**
	 * S3 버킷에 파일을 업로드 한다.
	 * @param multipartFile 업로드할 파일 데이터를 담고 있는 Spring의 MultipartFile 객체다.
	 * @param bucketName 업로드할 S3 버킷의 이름이다.
	 * @param folder 파일을 저장할 S3 버킷 내의 폴더 경로다.
	 * @param filename S3에 저장될 파일 이름이다.
	 */
	public void uploadFile(MultipartFile multipartFile, String bucketName, String folder, String filename) {
		
		try {
			// 파일이 S3에 저장될 경로와 파일 이름을 결합한 문자열입니다.
			String s3Filename = folder + "/" + filename;
			
			/*
			 * PutObjectRequest
			 * 	- S3에 객체를 업로드할 때 필요한 요청 정보를 담는 객체다.
			 * 	- 주요 메소드
			 * 		- .bucket(String bucketName) : 업로드할 대상 버킷의 이름을 설정한다.
			 * 		- .key(String s3Filename) : S3 버킷 내에서 파일이 저장될 전체 경로다.
			 * 		- .contentType(String contentType) : 파일의 MIME 타입을 설정한다.
			 * 		- .contentLength(long size) : 파일의 크기를 설정한다.
			 */
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Filename)
				.contentType(multipartFile.getContentType())
				.contentLength(multipartFile.getSize())
				.build();
			
			/*
			 * .putObject(PutObjectRequest putObjectRequest, RequestBody requestBody)
			 * 	- S3에 객체를 업로드하는 메서드다.
			 * 	- 매개변수
			 * 		- PutObjectRequest : 업로드 요청 정보를 담은 객체다.
			 * 		- RequestBody : 파일의 데이터를 바이트 배열로 변환하여 요청 본문에 담는다.
			 */
			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(multipartFile.getBytes()));
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}		
	}
	
	/**
	 * AWS S3에서 파일을 다운로드하는 메서드다.
	 * @param bucketName 다운로드할 S3 버킷의 이름이다.
	 * @param folder S3 버킷 내에서 파일이 위치한 폴더 경로다.
	 * @param filename 다운로드할 파일의 이름이다.
	 * @return 파일 데이터를 바이트 배열 형태로 감싼 ByteArrayResource 객체
	 */
	public ByteArrayResource downloadFile(String bucketName, String folder, String filename) {
		// 다운로드할 S3 객체의 전체 경로다.
		String s3Filename = folder + "/" + filename;
		
		/*
		 * GetObjectRequest
		 * 	- S3에서 객체를 가져올 때 필요한 요청 정보를 담는 객체다.
		 * 	- 주요 메소드
		 * 		- .bucket(String bucketName): S3 버킷 이름을 설정한다.
		 * 		- .key(String s3Filename): 다운로드할 파일의 S3 경로를 설정한다.
		 */
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Filename)
			.build();
		
		/*
		 * ResponseInputStream<GetObjectResponse> getObject(GetObjectRequest getObjectRequest)
		 * 	- S3에서 파일을 가져와 입력 스트림(InputStream)으로 반환한다.
		 */
		try (InputStream inputStream = s3Client.getObject(getObjectRequest);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 스트림을 읽고 바이트 배열로 변환
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            // 바이트 배열을 감싸는 Spring의 리소스 객체로, HTTP 응답 등에서 파일을 반환할 때 사용한다.
            return new ByteArrayResource(outputStream.toByteArray());

		} catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}
	
	/**
	 *  AWS S3 버킷에서 파일을 삭제하는 메서드다.
	 * @param bucketName 파일이 저장된 S3 버킷의 이름이다.
	 * @param folder S3 버킷 내에서 파일이 위치한 폴더다.
	 * @param filename 삭제할 파일의 이름이다.
	 */
	public void deleteFile(String bucketName, String folder, String filename) {
		// S3에서 파일을 찾기 위한 전체 경로다.
		String s3Filename = folder + "/" + filename;
		
		/*
		 * DeleteObjectRequest
		 * 	- S3에 저장된 객체를 삭제할 때 필요한 요청정보를 담는 객체다.
		 * 	- 주요 메소드
		 * 		- .bucket(String bucketName): S3 버킷 이름을 설정한다.
		 * 		- .key(String s3Filename): 삭제할 파일의 S3 경로를 설정한다.
		 */
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Filename)
			.build();
		
		try {
			/*
			 * DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest)
			 * 	- AWS S3에서 지정된 파일을 삭제한다.
			 * 	- 매개변수
			 * 		- DeleteObjectRequest: 삭제에 필요한 정보가 포함된 객체다.
			 */
			s3Client.deleteObject(deleteObjectRequest);
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}		
		
	}
}
