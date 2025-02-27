package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

import java.util.Map;


public class SubscribeToSNSLambda implements RequestHandler<Map<String, String>, String> {
    private final SnsClient snsClient;

    public SubscribeToSNSLambda() {
        snsClient = SnsClient.create();
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        context.getLogger().log("Event: " + event);
        String topicArn = event.get("TopicArn");
        String email = event.get("Email");

        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(email)
                .build();

        snsClient.subscribe(request);

        return "Successfully subscribed to " + topicArn + " with email " + email;
    }
}