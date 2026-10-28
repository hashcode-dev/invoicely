package com.hashcode.invoicely.dto.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.client.ClientDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("client_id")
    private UUID clientId;

    private String name;

    private String number;

    @JsonProperty("task_rate")
    @Builder.Default
    private BigDecimal taskRate = BigDecimal.ZERO;

    @JsonProperty("budgeted_hours")
    @Builder.Default
    private BigDecimal budgetedHours = BigDecimal.ZERO;

    @JsonProperty("current_hours")
    @Builder.Default
    private BigDecimal currentHours = BigDecimal.ZERO;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("private_notes")
    private String privateNotes;

    @JsonProperty("public_notes")
    private String publicNotes;

    private String color;

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

    @JsonProperty("custom_value3")
    private String customValue3;

    @JsonProperty("custom_value4")
    private String customValue4;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    private ClientDto client;

    @Builder.Default
    private List<Object> documents = new ArrayList<>();
}
