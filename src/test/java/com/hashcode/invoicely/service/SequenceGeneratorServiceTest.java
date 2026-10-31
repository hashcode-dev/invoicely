package com.hashcode.invoicely.service;

import com.hashcode.invoicely.model.common.DatabaseSequence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.hashcode.invoicely.service.impl.SequenceGeneratorServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequenceGeneratorServiceTest {

    @Mock
    private MongoOperations mongoOperations;

    @InjectMocks
    private SequenceGeneratorServiceImpl sequenceGeneratorService;

    @Test
    @DisplayName("Should format sequence with 3 leading zeros when value <= 999")
    void shouldFormatSequenceWithPadding() {
        DatabaseSequence sequence = new DatabaseSequence();
        sequence.setId("customer_seq");
        sequence.setSeq(5L);

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(DatabaseSequence.class)
        )).thenReturn(sequence);

        String result = sequenceGeneratorService.generateSequence("customer_seq");
        assertThat(result).isEqualTo("005");
    }

    @Test
    @DisplayName("Should return raw sequence string without padding when value > 999")
    void shouldFormatSequenceWithoutPaddingWhenLarge() {
        DatabaseSequence sequence = new DatabaseSequence();
        sequence.setId("invoice_seq");
        sequence.setSeq(1250L);

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(DatabaseSequence.class)
        )).thenReturn(sequence);

        String result = sequenceGeneratorService.generateSequence("invoice_seq");
        assertThat(result).isEqualTo("1250");
    }

    @Test
    @DisplayName("Should default to 001 if sequence record returns null")
    void shouldDefaultToOneWhenRecordNull() {
        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(DatabaseSequence.class)
        )).thenReturn(null);

        String result = sequenceGeneratorService.generateSequence("new_seq");
        assertThat(result).isEqualTo("001");
    }
}
