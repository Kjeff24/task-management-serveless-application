package org.example;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserConfigType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageTemplateType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserPoolRequest;

public class UpdateInviteMessageLambda implements RequestHandler<CloudFormationCustomResourceEvent, Void> {
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    public UpdateInviteMessageLambda() {
        cognitoIdentityProviderClient = CognitoIdentityProviderClient.create();
    }

    @Override
    public Void handleRequest(CloudFormationCustomResourceEvent event, Context context) {
        String requestType = event.getRequestType();

        if ("Delete".equalsIgnoreCase(requestType)) {
            return null;
        }

        String userPoolId = event.getResourceProperties().get("UserPoolId").toString();
        String userPoolDomain = event.getResourceProperties().get("UserPoolDomain").toString();
        String userPoolClient = event.getResourceProperties().get("UserPoolClient").toString();
        String frontendProdHost = event.getResourceProperties().get("FrontendProdHost").toString();
        String region = event.getResourceProperties().get("Region").toString();

        try {
            MessageTemplateType inviteMessageTemplate = MessageTemplateType.builder()
                    .emailMessage(String.format("""
                                    Hello {username},

                                    Welcome to our Task Management System! Your temporary password is {####}. \
                                    Please use it to sign in and reset your password.

                                    Click here to sign in: https://%s.auth.%s.amazoncognito.com/login?client_id=%s&response_type=code&redirect_uri=%s""",
                    userPoolDomain, region, userPoolClient, frontendProdHost))
                    .emailSubject("Welcome to Task Management System")
                    .build();

            AdminCreateUserConfigType adminCreateUserConfig = AdminCreateUserConfigType.builder()
                    .allowAdminCreateUserOnly(true)
                    .inviteMessageTemplate(inviteMessageTemplate)
                    .build();

            UpdateUserPoolRequest updateUserPoolRequest = UpdateUserPoolRequest.builder()
                    .userPoolId(userPoolId)
                    .adminCreateUserConfig(adminCreateUserConfig)
                    .build();

            cognitoIdentityProviderClient.updateUserPool(updateUserPoolRequest);

            context.getLogger().log("Successfully updated the UserPool with new InviteMessageTemplate");

        } catch (Exception e) {
            context.getLogger().log("Error updating the UserPool: " + e.getMessage());
        }

        return null;
    }
}
