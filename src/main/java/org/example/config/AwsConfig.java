package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {
    @Value("${app.aws.region}")
    private String awsRegion;

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder().region(Region.of(awsRegion)).build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder().region(Region.of(awsRegion)).build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder().region(Region.of(awsRegion)).build();
    }


}
