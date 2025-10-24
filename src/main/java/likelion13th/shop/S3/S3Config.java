package likelion13th.shop.S3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
public class S3Config {
    private final S3Properties s3properties;

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(
                s3properties.getAccessKey(),
                s3properties.getSecretKey()
        );

        return AmazonS3ClientBuilder.standard()
                .withRegion(s3properties.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
}
