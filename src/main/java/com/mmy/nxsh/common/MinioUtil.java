package com.mmy.nxsh.common;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 简单的 MinIO 工具类，只做上传，方便 MedicineService 使用。
 */
@Slf4j
@Component
public class MinioUtil implements InitializingBean {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.external-url:}")
    private String externalUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient client;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // 自动创建 bucket（如不存在）
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO bucket '{}' 已自动创建", bucket);
        }

        // 设置 bucket 为公开读，让真机可直接通过 URL 访问文件
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
        client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        log.info("MinIO bucket '{}' 已设置为公开读", bucket);
    }

    /**
     * 上传文件到 MinIO，返回可访问的 URL。
     */
    public String upload(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String suffix = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String objectName = buildObjectName(subDir, suffix);

        try (InputStream in = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(args);
        } catch (MinioException e) {
            throw new RuntimeException("上传到 MinIO 失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("上传到 MinIO 失败", e);
        }

        // 返回外部可访问的 URL（优先用 externalUrl，没配则用 endpoint）
        String baseUrl = (externalUrl != null && !externalUrl.isBlank()) ? externalUrl : endpoint;
        return baseUrl + "/" + bucket + "/" + objectName;
    }

    /**
     * 上传字节数组到 MinIO，返回可访问的 URL。
     */
    public String uploadBytes(byte[] data, String subDir, String suffix, String contentType) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("数据不能为空");
        }
        String objectName = buildObjectName(subDir, suffix);

        try (InputStream in = new java.io.ByteArrayInputStream(data)) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(in, data.length, -1)
                    .contentType(contentType)
                    .build();
            client.putObject(args);
        } catch (MinioException e) {
            throw new RuntimeException("上传到 MinIO 失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("上传到 MinIO 失败", e);
        }

        String baseUrl = (externalUrl != null && !externalUrl.isBlank()) ? externalUrl : endpoint;
        return baseUrl + "/" + bucket + "/" + objectName;
    }

    private String buildObjectName(String subDir, String suffix) {
        String cleanDir = (subDir == null || subDir.isBlank()) ? "" : subDir.trim();
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        if (!cleanDir.isEmpty()) {
            if (!cleanDir.endsWith("/")) {
                cleanDir = cleanDir + "/";
            }
            return cleanDir + fileName;
        }
        return fileName;
    }
}
