package com.influencerapp.test.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Creates temporary valid and invalid media files on-the-fly for multipart upload testing.
 */
@Slf4j
public final class MediaFileProvider {

    // Standard minimal MP4 container header (ftyp box)
    private static final byte[] MP4_HEADER = new byte[]{
            0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, // size 32, 'ftyp'
            0x69, 0x73, 0x6F, 0x6D, 0x00, 0x00, 0x02, 0x00, // major 'isom', minor 512
            0x69, 0x73, 0x6F, 0x6D, 0x69, 0x73, 0x6F, 0x32, // compatible 'isom', 'iso2'
            0x61, 0x76, 0x63, 0x31, 0x6D, 0x70, 0x34, 0x31  // 'avc1', 'mp41'
    };

    private MediaFileProvider() {
    }

    /**
     * Creates a dummy valid MP4 file of the given approximate size in bytes.
     */
    public static File createSampleMp4(int sizeBytes) throws IOException {
        File tempFile = Files.createTempFile("test-video-", ".mp4").toFile();
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(MP4_HEADER);
            int remaining = Math.max(0, sizeBytes - MP4_HEADER.length);
            if (remaining > 0) {
                byte[] chunk = new byte[Math.min(remaining, 64 * 1024)];
                Arrays.fill(chunk, (byte) 0xAA);
                while (remaining > 0) {
                    int toWrite = Math.min(remaining, chunk.length);
                    fos.write(chunk, 0, toWrite);
                    remaining -= toWrite;
                }
            }
        }
        log.debug("Created temporary sample MP4 at {} (size: {} bytes)", tempFile.getAbsolutePath(), tempFile.length());
        return tempFile;
    }

    /**
     * Creates an invalid text file to test MIME type validation failure.
     */
    public static File createInvalidTextFile() throws IOException {
        File tempFile = Files.createTempFile("invalid-upload-", ".txt").toFile();
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "This is not a valid video file. MIME validation should reject it.");
        return tempFile;
    }
}
