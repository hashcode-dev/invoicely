package com.hashcode.invoicely.service;

import com.hashcode.invoicely.service.impl.SequenceGeneratorServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SequenceGeneratorRelationalTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should increment and format relational sequence using JdbcTemplate")
    void shouldIncrementRelationalSequence() {
        SequenceGeneratorServiceImpl service = new SequenceGeneratorServiceImpl(jdbcTemplate, null);

        when(jdbcTemplate.update(startsWith("UPDATE"), eq("invoice_123"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(startsWith("SELECT"), eq(Long.class), eq("invoice_123"))).thenReturn(42L);

        String seq = service.generateSequence("invoice_123");
        assertThat(seq).isEqualTo("042");
    }

    @Test
    @DisplayName("Should insert new sequence record when update returns zero rows")
    void shouldInsertInitialRelationalSequence() {
        SequenceGeneratorServiceImpl service = new SequenceGeneratorServiceImpl(jdbcTemplate, null);

        when(jdbcTemplate.update(startsWith("UPDATE"), eq("payment_new"))).thenReturn(0);
        when(jdbcTemplate.update(startsWith("INSERT"), eq("payment_new"))).thenReturn(1);

        String seq = service.generateSequence("payment_new");
        assertThat(seq).isEqualTo("001");
    }
}
