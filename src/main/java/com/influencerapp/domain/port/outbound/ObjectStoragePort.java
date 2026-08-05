package com.influencerapp.domain.port.outbound;

import java.io.InputStream;
/**
 * Outbound port defining the contract for object storage operations.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface ObjectStoragePort {

    void store(InputStream inputStream, String path, String contentType);

    InputStream retrieve(String path);
}
