package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.model.common.DatabaseSequence;
import com.hashcode.invoicely.service.SequenceGeneratorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("sequenceGeneratorService")
@Getter
public class SequenceGeneratorServiceImpl implements SequenceGeneratorService {

    private final JdbcTemplate jdbcTemplate;
    private final MongoOperations mongoOperations;

    @Autowired
    public SequenceGeneratorServiceImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) MongoOperations mongoOperations) {
        this.jdbcTemplate = jdbcTemplate;
        this.mongoOperations = mongoOperations;
    }

    public SequenceGeneratorServiceImpl(MongoOperations mongoOperations) {
        this(null, mongoOperations);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateSequence(String seqName) {
        Long seq = null;

        // 1. Try PostgreSQL Relational Atomic Sequence first
        if (jdbcTemplate != null) {
            try {
                int updated = jdbcTemplate.update(
                        "UPDATE invoicely.system_sequences SET seq = seq + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                        seqName
                );
                if (updated == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO invoicely.system_sequences (id, seq, updated_at) VALUES (?, 1, CURRENT_TIMESTAMP)",
                            seqName
                    );
                    seq = 1L;
                } else {
                    seq = jdbcTemplate.queryForObject(
                            "SELECT seq FROM invoicely.system_sequences WHERE id = ?",
                            Long.class,
                            seqName
                    );
                }
            } catch (Exception e) {
                log.debug("Relational sequence generation fallback for {}: {}", seqName, e.getMessage());
            }
        }

        // 2. Fallback to MongoDB if relational not initialized or for document store
        if (seq == null && mongoOperations != null) {
            try {
                DatabaseSequence counter = mongoOperations.findAndModify(
                        Query.query(Criteria.where("_id").is(seqName)),
                        new Update().inc("seq", 1),
                        FindAndModifyOptions.options().returnNew(true).upsert(true),
                        DatabaseSequence.class
                );
                seq = (counter != null ? counter.getSeq() : 1L);
            } catch (Exception e) {
                log.error("MongoDB sequence generation failed for {}: {}", seqName, e.getMessage());
            }
        }

        if (seq == null) {
            seq = 1L;
        }

        return formatSequence(seq);
    }

    private String formatSequence(long seq) {
        if (seq <= 999) {
            return String.format("%03d", seq);
        } else {
            return String.valueOf(seq);
        }
    }
}
