package com.lxc.springbootinit.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云对象存储客户端
 */
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class CosClientConfig {


	private String endPoint;

	private String accessKeyId;

	private String accessKeySecret;

	private String fileHost;

	private String bucketName;


	// 将OSS 客户端交给Spring容器托管
	@Bean
	public OSS OSSClient() {
		return new OSSClient(endPoint, accessKeyId, accessKeySecret);
	}

}