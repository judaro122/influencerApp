package com.influencerapp.domain.port.outbound;

import java.io.InputStream;

public interface ObjectStoragePort {

    void store(InputStream inputStream, String path, String contentType);

    InputStream retrieve(String path);
}
