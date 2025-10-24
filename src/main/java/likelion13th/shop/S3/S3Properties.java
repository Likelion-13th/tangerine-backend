package likelion13th.shop.S3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws")
public class S3Properties {
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region;
}