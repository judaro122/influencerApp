package com.influencerapp.application.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaginatedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}