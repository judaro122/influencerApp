package com.influencerapp.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
/**
 * Data transfer object wrapping a paginated collection of results.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class PaginatedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}