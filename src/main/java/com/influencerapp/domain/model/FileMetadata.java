package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class FileMetadata {

    String filename;
    long size;
    String mimeType;
    String checksum;
}