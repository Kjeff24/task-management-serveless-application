package org.example.mapper;

import org.example.dto.UserResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

@Service
public class UserMapper {

    public UserResponse toUserResponse(UserType user) {
        return UserResponse.builder()
                .username(user.username())
                .enabled(user.enabled())
                .build();
    }
}
