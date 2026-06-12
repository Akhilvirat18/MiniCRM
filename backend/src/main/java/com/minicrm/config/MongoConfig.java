package com.minicrm.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
@Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableMongoRepositories(basePackages = "com.minicrm.repository.mongo.db")
public class MongoConfig {
    // Dynamically enables MongoDB support when crm.database.mode=mongodb
}
