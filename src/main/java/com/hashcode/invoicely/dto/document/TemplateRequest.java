package com.hashcode.invoicely.dto.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateRequest {

    private String body;

    private String subject;

    private String entity;

    @JsonProperty("entity_id")
    private String entityId;

    private String template;

    @JsonProperty("cc_email")
    private String ccEmail;
}
