package com.example;

import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumes verification events from SQS.
 *
 * <p>Ordinary production code: it long-polls the queue with the AWS SDK and knows nothing
 * about Stubborn. The consumer-side test drives it by having the stub runner publish a
 * triggered stub to the same real queue, so what is exercised here is the listener a
 * consumer actually ships.
 */
@Component
public class VerificationListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationListener.class);

    static final String QUEUE_NAME = "verifications";

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final Set<String> received = ConcurrentHashMap.newKeySet();
    private final ExecutorService poller = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    public VerificationListener(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void start() {
        String queueUrl = this.sqsClient.createQueue(b -> b.queueName(QUEUE_NAME)).queueUrl();
        this.poller.submit(() -> pollUntilStopped(queueUrl));
    }

    private void pollUntilStopped(String queueUrl) {
        while (this.running) {
            try {
                List<Message> messages = this.sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(1)
                        .build()).messages();
                for (Message message : messages) {
                    handle(message);
                    this.sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build());
                }
            }
            catch (RuntimeException ex) {
                if (this.running) {
                    log.warn("Polling the verifications queue failed", ex);
                }
            }
        }
    }

    private void handle(Message message) {
        try {
            VerificationEvent event = this.objectMapper.readValue(message.body(), VerificationEvent.class);
            this.received.add(event.bookName());
            log.info("Received a verification event for [{}]", event.bookName());
        }
        catch (Exception ex) {
            log.warn("Could not read the verification event from [{}]", message.body(), ex);
        }
    }

    public boolean hasReceived(String bookName) {
        return this.received.contains(bookName);
    }

    @PreDestroy
    void stop() {
        this.running = false;
        this.poller.shutdownNow();
        try {
            this.poller.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
