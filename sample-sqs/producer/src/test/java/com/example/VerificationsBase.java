package com.example;

import java.net.URI;

import com.example.sqs.SqsMessage;
import com.example.sqs.StubbornSqsMessageVerifierReceiver;
import com.example.sqs.StubbornSqsMessageVerifierSender;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;

import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

/**
 * Base class for the generated producer-side contract tests.
 *
 * <p>
 * The generated test triggers {@link #verificationTriggered()} and then asserts that a
 * message matching the contract really landed on the {@code verifications} queue of a
 * <strong>real SQS</strong>, provided by LocalStack.
 *
 * <p>
 * <strong>Why this class wires the messaging beans by hand.</strong> Stubborn ships Spring
 * Boot autoconfiguration for the transports it knows (Kafka, JMS, Camel, Spring
 * Integration), and there is none for SQS yet. That is not a blocker, because every one of
 * those backends — and the no-op fallback — declares its beans
 * {@code @ConditionalOnMissingBean(MessageVerifierSender.class)}, so supplying our own
 * simply wins and the rest back off. This is the supported extension point for a transport
 * the library does not know about.
 *
 * <p>
 * <strong>The {@link ContractVerifierMessaging} bean is not optional.</strong> The no-op
 * autoconfiguration builds that bean from {@code NoOpStubMessages} rather than from
 * whatever sender and receiver are in the context, so declaring only the sender and
 * receiver would leave verification running against the no-op: every send swallowed, every
 * receive empty, and a contract test that passes without ever touching the broker. It has
 * to be declared here, over the real pair.
 */
@SpringBootTest(classes = { ProducerApplication.class, VerificationsBase.SqsTestConfiguration.class })
@AutoConfigureMessageVerifier
@Import(VerificationsBase.SqsTestConfiguration.class)
public abstract class VerificationsBase {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.8");

    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.SQS);

    static {
        LOCALSTACK.start();
    }

    @Autowired
    VerificationService verificationService;

    /** Invoked by the generated test, via the contract's {@code triggeredBy}. */
    public void verificationTriggered() {
        this.verificationService.sendVerification(new VerificationEvent("foo"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SqsTestConfiguration {

        @Bean(destroyMethod = "close")
        SqsClient sqsClient() {
            return SqsClient.builder()
                    .endpointOverride(URI.create(
                            LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                    .region(Region.of(LOCALSTACK.getRegion()))
                    .build();
        }

        @Bean
        MessageVerifierSender<SqsMessage> sqsMessageVerifierSender(SqsClient sqsClient) {
            return new StubbornSqsMessageVerifierSender(sqsClient);
        }

        @Bean
        MessageVerifierReceiver<SqsMessage> sqsMessageVerifierReceiver(SqsClient sqsClient) {
            return new StubbornSqsMessageVerifierReceiver(sqsClient);
        }

        /**
         * Must be declared explicitly — see the class javadoc. Without it the no-op
         * autoconfiguration supplies a {@code ContractVerifierMessaging} built from
         * {@code NoOpStubMessages}, and the contract test would pass while never reading
         * the queue.
         */
        @Bean
        ContractVerifierMessaging<SqsMessage> contractVerifierMessaging(MessageVerifierSender<SqsMessage> sender,
                MessageVerifierReceiver<SqsMessage> receiver) {
            return new ContractVerifierMessaging<>(sender, receiver);
        }
    }
}
