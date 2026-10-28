package com.hashcode.invoicely.dto.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.client.ClientDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("project_id")
    private UUID projectId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    @JsonProperty("status_id")
    private UUID statusId;

    private String number;

    private String description;

    @Builder.Default
    private Long duration = 0L;

    @JsonProperty("is_running")
    @Builder.Default
    private boolean isRunning = false;

    @JsonProperty("time_log")
    @Builder.Default
    private String timeLog = "[]";

    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @JsonProperty("is_date_based")
    @Builder.Default
    private boolean isDateBased = false;

    private LocalDate date;

    @JsonProperty("calculated_start_date")
    private Instant calculatedStartDate;

    @JsonProperty("status_order")
    @Builder.Default
    private Integer statusOrder = 0;

    @JsonProperty("status_sort_order")
    @Builder.Default
    private Integer statusSortOrder = 0;

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

    private ProjectDto project;

    private TaskStatusDto status;

    @Builder.Default
    private List<Object> documents = new ArrayList<>();
}
