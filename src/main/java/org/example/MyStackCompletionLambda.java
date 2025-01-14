package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.example.util.PasswordGenerator;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserConfigType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageTemplateType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserPoolRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyStackCompletionLambda implements RequestHandler<CloudFormationCustomResourceEvent, Void> {
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private final SnsClient snsClient;

    public MyStackCompletionLambda() {
        cognitoIdentityProviderClient = CognitoIdentityProviderClient.create();
        snsClient = SnsClient.create();
    }

    @Override
    public Void handleRequest(CloudFormationCustomResourceEvent event, Context context) {
        String responseUrl = event.getResponseUrl();
        String status = "SUCCESS";
        Map<String, Object> responseData = new HashMap<>();

        try {
            if ("Delete".equalsIgnoreCase(event.getRequestType())) {
                sendResponse(responseUrl, event, context, status, responseData);
                return null;
            }
            String userPoolId = event.getResourceProperties().get("UserPoolId").toString();

            addInviteMessageTemplate(event, userPoolId, responseData, context);
            createAdminUser(event, userPoolId, responseData, context);

        } catch (Exception e) {
            status = "FAILED";
            context.getLogger().log("Error: " + e.getMessage());
            responseData.put("Error", e.getMessage());
        }

        sendResponse(responseUrl, event, context, status, responseData);
        return null;
    }

    private void createAdminUser(CloudFormationCustomResourceEvent event, String userPoolId, Map<String, Object> responseData, Context context) {
        String adminEmail = event.getResourceProperties().get("AdminEmail").toString();
        String adminGroup = event.getResourceProperties().get("AdminGroup").toString();
        String closedTaskTopicArn = event.getResourceProperties().get("ClosedTaskTopicArn").toString();
        String taskCompleteTopicArn = event.getResourceProperties().get("TaskCompleteTopicArn").toString();

        if (adminEmail != null && !adminEmail.trim().equals("None") && !adminEmail.isEmpty()) {
            try {
                cognitoIdentityProviderClient.adminGetUser(AdminGetUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(adminEmail)
                        .build());

                context.getLogger().log("User already exists: " + adminEmail);
                responseData.put("Message", "User already exists: " + adminEmail);
            } catch (UserNotFoundException e) {
                List<AttributeType> userAttributes = new ArrayList<>();
                userAttributes.add(AttributeType.builder().name("email").value(adminEmail).build());
                userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());

                AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(adminEmail)
                        .userAttributes(userAttributes)
                        .temporaryPassword(PasswordGenerator.generatePassword(9))
                        .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                        .build();

                cognitoIdentityProviderClient.adminCreateUser(createUserRequest);

                AdminAddUserToGroupRequest addUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(adminEmail)
                        .groupName(adminGroup)
                        .build();

                cognitoIdentityProviderClient.adminAddUserToGroup(addUserToGroupRequest);

                subscribeToTopic(closedTaskTopicArn, adminEmail);
                subscribeToTopic(taskCompleteTopicArn, adminEmail);

                context.getLogger().log("Admin created successfully");
                responseData.put("Message", "Admin created successfully");
            }
        }
    }


    private void subscribeToTopic(String topicArn, String endpoint) {
        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(endpoint)
                .build();
        snsClient.subscribe(request);
    }

    private void addInviteMessageTemplate(CloudFormationCustomResourceEvent event, String userPoolId, Map<String, Object> responseData, Context context) {
        String domain = event.getResourceProperties().get("UserPoolDomain").toString();
        String clientId = event.getResourceProperties().get("UserPoolClient").toString();
        String frontendHost = event.getResourceProperties().get("FrontendProdHost").toString();
        String region = event.getResourceProperties().get("Region").toString();
        MessageTemplateType inviteMessageTemplate = MessageTemplateType.builder()
                .emailMessage(String.format("""
                                Hello {username}, Welcome to our Task Management System! \

                                Your temporary password is {####}\

                                Click here to sign in: \
                                https://%s.auth.%s.amazoncognito.com/login?client_id=%s&response_type=code&redirect_uri=%s""",
                        domain, region, clientId, frontendHost))
                .emailSubject("Welcome to Task Management System")
                .build();

        cognitoIdentityProviderClient.updateUserPool(UpdateUserPoolRequest.builder()
                .userPoolId(userPoolId)
                .adminCreateUserConfig(AdminCreateUserConfigType.builder()
                        .allowAdminCreateUserOnly(true)
                        .inviteMessageTemplate(inviteMessageTemplate)
                        .build())
                .build());

        context.getLogger().log("UserPool updated successfully");
        responseData.put("Message", "UserPool updated successfully");
    }

    private void sendResponse(String url, CloudFormationCustomResourceEvent event, Context context, String status, Map<String, Object> data) {
        LambdaLogger logger = context.getLogger();
        ObjectMapper objectMapper = new ObjectMapper();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            Map<String, Object> responseBody = Map.of(
                    "Status", status,
                    "Reason", "See the details in CloudWatch Log Stream: " + context.getLogStreamName(),
                    "PhysicalResourceId", event.getPhysicalResourceId() != null ? event.getPhysicalResourceId() : context.getLogStreamName(),
                    "StackId", event.getStackId(),
                    "RequestId", event.getRequestId(),
                    "LogicalResourceId", event.getLogicalResourceId(),
                    "Data", data
            );

            try {
                StringEntity entity = new StringEntity(objectMapper.writeValueAsString(responseBody));
                HttpPut request = new HttpPut(url);
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json");

                httpClient.execute(request, response -> {
                    EntityUtils.consume(response.getEntity());
                    logger.log("Response sent to CloudFormation successfully.");
                    return null;
                });
                logger.log("Response sent to CloudFormation successfully.");
            } catch (IOException e) {
                logger.log("Failed to send response to CloudFormation: " + e.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
