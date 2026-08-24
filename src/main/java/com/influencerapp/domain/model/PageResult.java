package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import java.util.List;


/**
 * Immutable value object representing a paginated collection of results.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Value
@AllArgsConstructor
@Getter
public class PageResult<T> {

    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
