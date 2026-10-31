package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.model.common.DatabaseSequence;
import com.hashcode.invoicely.service.SequenceGeneratorService;
import lombok.Getter;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service("sequenceGeneratorService")
@Getter
public class SequenceGeneratorServiceImpl implements SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    public SequenceGeneratorServiceImpl(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public String generateSequence(String seqName) {
        DatabaseSequence counter = getMongoOperations().findAndModify(
                Query.query(Criteria.where("_id").is(seqName)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );

        long seq = (counter != null ? counter.getSeq() : 1);

        // Format with leading zeros only if <= 999
        if (seq <= 999) {
            return String.format("%03d", seq); // always 3 digits
        } else {
            return String.valueOf(seq); // no padding for numbers > 999
        }
    }
}
