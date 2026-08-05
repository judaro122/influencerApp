package com.influencerapp.domain.model;
/**
 * Enumeration of possible states in the video publishing lifecycle.
 *
 * @author judaro122
 * @since 1.0.0
 */



public enum VideoStatus {
    RECEIVED,
    PROCESSING,
    UPLOADING,
    PUBLISHED,
    FAILED
}