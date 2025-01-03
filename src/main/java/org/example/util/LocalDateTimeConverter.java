package org.example.util;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");

    @Override
    public AttributeValue transformFrom(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return AttributeValue.builder().nul(true).build();
        }
        // Store as a string in ISO format with nanoseconds
        return AttributeValue.builder().s(localDateTime.format(FORMATTER)).build();
    }

    @Override
    public LocalDateTime transformTo(AttributeValue attributeValue) {
        if (attributeValue.s() == null) {
            return null;
        }
        // Parse the stored string back into LocalDateTime
        return LocalDateTime.parse(attributeValue.s(), FORMATTER);
    }

    @Override
    public EnhancedType<LocalDateTime> type() {
        return EnhancedType.of(LocalDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
