package com.influencerapp.infrastructure.adapter.storage;

import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
/**
 * Adapter implementing object storage operations using MinIO (S3-compatible)
 * with streaming multipart upload and tenant-prefixed paths.
 *
 * @author judaro122
 * @since 1.0.0
 */
public class MinioStorageAdapter implements ObjectStoragePort {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private final MinioClient minioClient;

    @Override
    public void store(InputStream inputStream, String path, String contentType) {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(inputStream, -1, 5 * 1024 * 1024)
                    .contentType(contentType)
                    .build();
            minioClient.putObject(args);
        } catch (MinioException e) {
            throw new RuntimeException("Failed to store object in MinIO: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store object in MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream retrieve(String path) {
        try {
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
        } catch (MinioException e) {
            throw new RuntimeException("Failed to retrieve object from MinIO: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve object from MinIO: " + e.getMessage(), e);
        }
    }
}
