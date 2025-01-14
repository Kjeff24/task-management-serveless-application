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
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserConfigType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageTemplateType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserPoolRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UpdateInviteMessageLambda implements RequestHandler<CloudFormationCustomResourceEvent, Void> {
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    public UpdateInviteMessageLambda() {
        cognitoIdentityProviderClient = CognitoIdentityProviderClient.create();
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
            String domain = event.getResourceProperties().get("UserPoolDomain").toString();
            String clientId = event.getResourceProperties().get("UserPoolClient").toString();
            String frontendHost = event.getResourceProperties().get("FrontendProdHost").toString();
            String region = event.getResourceProperties().get("Region").toString();

            MessageTemplateType inviteMessageTemplate = MessageTemplateType.builder()
                    .emailMessage(String.format("Hello {username}, Welcome to our Task Management System! "
                                    + "Your temporary password is {####}. Click here to sign in: "
                                    + "https://%s.auth.%s.amazoncognito.com/login?client_id=%s&response_type=code&redirect_uri=%s",
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
        } catch (Exception e) {
            status = "FAILED";
            context.getLogger().log("Error: " + e.getMessage());
            responseData.put("Error", e.getMessage());
        }

        sendResponse(responseUrl, event, context, status, responseData);
        return null;
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
