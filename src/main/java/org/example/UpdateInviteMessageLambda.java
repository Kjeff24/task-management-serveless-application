package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserConfigType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageTemplateType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserPoolRequest;

import java.net.HttpURLConnection;
import java.net.URL;
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
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");

            String responseBody = String.format("""
                    {
                        "Status": "%s",
                        "Reason": "Check logs for details",
                        "PhysicalResourceId": "%s",
                        "StackId": "%s",
                        "RequestId": "%s",
                        "LogicalResourceId": "%s",
                        "Data": %s
                    }""", status, event.getLogicalResourceId(), event.getStackId(), event.getRequestId(), event.getLogicalResourceId(), data);

            connection.getOutputStream().write(responseBody.getBytes());
            context.getLogger().log("Response sent: " + responseBody);
        } catch (Exception e) {
            context.getLogger().log("Failed to send response: " + e.getMessage());
        }
    }
}
