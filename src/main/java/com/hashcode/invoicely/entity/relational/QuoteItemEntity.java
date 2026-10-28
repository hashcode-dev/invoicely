package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quote_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private QuoteEntity quote;

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

    @Column(name = "tax_name1", length = 64)
    private String taxName1;

    @Column(name = "tax_rate1", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal taxRate1 = BigDecimal.ZERO;

    @Column(name = "sort_id")
    @Builder.Default
    private Integer sortId = 0;
}
