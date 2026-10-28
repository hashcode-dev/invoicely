package com.hashcode.invoicely.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Standard collection wrapper matching Invoicely Front-Desk GenericManyResponse contract:
 * { "data": [...], "meta": { "pagination": { "total": 100, "count": 20, "per_page": 20, ... } } }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericManyResponse<T> {

    @Builder.Default
    private List<T> data = Collections.emptyList();

    private Meta meta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Meta {
        private Pagination pagination;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pagination {
        private long total;
        private int count;

        @JsonProperty("per_page")
        private int perPage;

        @JsonProperty("current_page")
        private int currentPage;

        @JsonProperty("total_pages")
        private int totalPages;

        private Links links;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Links {
        private String previous;
        private String next;
    }

    public static <T> GenericManyResponse<T> of(List<T> data, long total, int page, int perPage) {
        int count = data != null ? data.size() : 0;
        int totalPages = perPage > 0 ? (int) Math.ceil((double) total / perPage) : 1;
        if (totalPages < 1) {
            totalPages = 1;
        }

        return GenericManyResponse.<T>builder()
                .data(data != null ? data : Collections.emptyList())
                .meta(Meta.builder()
                        .pagination(Pagination.builder()
                                .total(total)
                                .count(count)
                                .perPage(perPage)
                                .currentPage(page)
                                .totalPages(totalPages)
                                .links(Links.builder()
                                        .previous(page > 1 ? "?page=" + (page - 1) : null)
                                        .next(page < totalPages ? "?page=" + (page + 1) : null)
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
