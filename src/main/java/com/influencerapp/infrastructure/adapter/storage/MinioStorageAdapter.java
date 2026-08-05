package com.influencerapp.infrastructure.adapter.storage;

import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class MinioStorageAdapter implements ObjectStoragePort {

    @Override
    public void store(InputStream inputStream, String path, String contentType) {
        throw new UnsupportedOperationException("Not implemented in Phase 1");
    }

    @Override
    public InputStream retrieve(String path) {
        throw new UnsupportedOperationException("Not implemented in Phase 1");
    }
}
