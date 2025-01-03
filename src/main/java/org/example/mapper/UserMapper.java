package org.example.mapper;

import org.example.dto.UserResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

@Service
public class UserMapper {

    public UserResponse toUserResponse(UserType user) {
        return UserResponse.builder()
                .userId(user.username())
                .email(user.attributes().stream()
                        .filter(attribute -> "email".equals(attribute.name()))
                        .map(AttributeType::value)
                        .findFirst()
                        .orElse(null))
                .fullName(user.attributes().stream()
                        .filter(attribute -> "name".equals(attribute.name())) // Adjust attribute name if needed
                        .map(AttributeType::value)
                        .findFirst()
                        .orElse(null))
                .build();
    }
}
