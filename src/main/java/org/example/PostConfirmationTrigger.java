package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;


public class PostConfirmationTrigger implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {

    private final SfnClient stepFunctionsClient;
    private final String stateMachineArn;

    public PostConfirmationTrigger() {
        stepFunctionsClient = SfnClient.create();
        stateMachineArn = System.getenv("STATE_MACHINE_ARN");
    }

    @Override
    public CognitoUserPoolPostConfirmationEvent handleRequest(CognitoUserPoolPostConfirmationEvent event, Context context) {
        String userEmail = event.getRequest().getUserAttributes().get("email");

        StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                .stateMachineArn(stateMachineArn)
                .input("{\"userEmail\": \"" + userEmail + "\"}")
                .build();

        stepFunctionsClient.startExecution(executionRequest);
        return event;
    }
}