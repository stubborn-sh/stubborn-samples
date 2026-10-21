package com.example;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

/**
 * Publishes verification events to SQS.
 *
 * <p>Deliberately plain production code: it talks to the AWS SDK directly and knows
 * nothing about Stubborn. The contract is verified against what this actually puts on the
 * queue, which is the point — if the sample cheated by publishing through the contract
 * tooling, the test would only be checking itself.
 *
 * <p>It sets {@code contentType} explicitly because that is what the contract asserts and
 * what tells a consumer how to read the body.
 */
@Service
public class VerificationService {

    static final String QUEUE_NAME = "verifications";

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public VerificationService(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = sqsClient.createQueue(b -> b.queueName(QUEUE_NAME)).queueUrl();
    }

    public void sendVerification(VerificationEvent event) {
        try {
            this.sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(this.queueUrl)
                    .messageBody(this.objectMapper.writeValueAsString(event))
                    .messageAttributes(Map.of("contentType", MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("application/json")
                            .build()))
                    .build());
        }
        catch (Exception ex) {
            throw new IllegalStateException("Could not publish the verification event", ex);
        }
    }
}
