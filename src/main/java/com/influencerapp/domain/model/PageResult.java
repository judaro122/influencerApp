package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.util.List;

@Value
@AllArgsConstructor
/**
 * Immutable value object representing a paginated collection of results.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class PageResult<T> {

    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
