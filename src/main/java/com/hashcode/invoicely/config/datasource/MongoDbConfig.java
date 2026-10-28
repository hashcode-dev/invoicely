package com.hashcode.invoicely.config.datasource;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Document Configuration.
 * Manages document persistence, audit timestamps, and document repository boundary.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.hashcode.invoicely.repository.document")
public class MongoDbConfig {
}
