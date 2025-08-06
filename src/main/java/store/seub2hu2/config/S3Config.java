package store.seub2hu2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// S3 임시 비활성화
// @Configuration
public class S3Config {

	@Value("${cloud.aws.credentials.access-key}")
	private String accessKey;

	@Value("${cloud.aws.credentials.secret-key}")
	private String secretKey;

	/*
	 * S3Client
	 *    - S3Client 객체는 AWS SDK for Java v2에서 제공하는 Amazon Simple Storage Service (S3)와 상호작용하기 위한 클라이언트다.
	 *    - S3Client 객체는 S3 버킷에 파일을 업로드, 다운로드, 삭제, 목록 조회 등의 작업을 수행할 수 있도록 지원한다.
	 *
	 *  StaticCredentialsProvider
	 *   - 정적 자격 증명(Access Key, Secret Key)을 제공하는 자격 증명 제공자다.
	 *   - AWS 서비스 접근을 위해 필요한 자격 증명을 설정한다.
	 *
	 *  AwsBasicCredentials
	 *   - AWS의 액세스 키와 시크릿 키를 담고 있는 객체다.
	 *   - AwsBasicCredentials.create(accessKey, secretKey)는 제공된 액세스 키와 시크릿 키를 기반으로 자격 증명 객체를 생성한다.
	 */
	//@Bean
	S3Client s3Client() {
		return S3Client.builder()
				.region(Region.AP_NORTHEAST_2)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey)))
				.build();
	}
}
