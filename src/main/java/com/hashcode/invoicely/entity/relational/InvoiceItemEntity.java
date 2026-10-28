package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "product_key", length = 128)
    private String productKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cost", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "discount", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "is_amount_discount", nullable = false)
    @Builder.Default
    private boolean isAmountDiscount = false;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "gross_line_total", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal grossLineTotal = BigDecimal.ZERO;

    @Column(name = "tax_name1", length = 64)
    private String taxName1;

    @Column(name = "tax_rate1", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal taxRate1 = BigDecimal.ZERO;

    @Column(name = "tax_name2", length = 64)
    private String taxName2;

    @Column(name = "tax_rate2", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal taxRate2 = BigDecimal.ZERO;

    @Column(name = "tax_name3", length = 64)
    private String taxName3;

    @Column(name = "tax_rate3", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal taxRate3 = BigDecimal.ZERO;

    @Column(name = "type_id", length = 32)
    @Builder.Default
    private String typeId = "1";

    @Column(name = "sort_id")
    @Builder.Default
    private Integer sortId = 0;

    @Column(name = "custom_value1")
    private String customValue1;

    @Column(name = "custom_value2")
    private String customValue2;

    @Column(name = "custom_value3")
    private String customValue3;

    @Column(name = "custom_value4")
    private String customValue4;
}
