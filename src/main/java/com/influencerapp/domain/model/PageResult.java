package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.util.List;

@Value
@AllArgsConstructor
public class PageResult<T> {

    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
