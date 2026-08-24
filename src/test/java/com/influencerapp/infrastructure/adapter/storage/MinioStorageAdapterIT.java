package com.influencerapp.infrastructure.adapter.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@Testcontainers
/**
 * MinioStorageAdapterIT component.
 *
 * @author judaro122
 * @since 1.0.0
 */
class MinioStorageAdapterIT {

    @Container
    protected static final MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .withUserName("minioadmin")
            .withPassword("minioadmin");

    @Autowired
    private MinioStorageAdapter adapter;

    @Value("${minio.bucket}")
    private String bucket;

    @Test
    @DisplayName("should store and retrieve object in MinIO")
    void shouldStoreAndRetrieveObject() throws Exception {
        String path = "tenants/tenant-1/videos/video-1/test.mp4";
        String content = "test content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        adapter.store(inputStream, path, "video/mp4");

        InputStream retrieved = adapter.retrieve(path);
        assertNotNull(retrieved);
        byte[] bytes = retrieved.readAllBytes();
        assertEquals(content, new String(bytes));
    }
}