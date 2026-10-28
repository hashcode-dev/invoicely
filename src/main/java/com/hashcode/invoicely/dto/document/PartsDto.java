package com.hashcode.invoicely.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartsDto {

    @Builder.Default
    private String includes = "";

    @Builder.Default
    private String header = "";

    @Builder.Default
    private String body = "";

    @Builder.Default
    private String product = "";

    @Builder.Default
    private String task = "";

    @Builder.Default
    private String footer = "";
}
