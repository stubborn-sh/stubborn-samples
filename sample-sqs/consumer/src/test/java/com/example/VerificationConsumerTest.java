package com.example;

import java.net.URI;
import java.time.Duration;

import com.example.sqs.SqsMessage;
import com.example.sqs.StubbornSqsMessageVerifierReceiver;
import com.example.sqs.StubbornSqsMessageVerifierSender;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer-side test: the stub runner publishes the producer's triggered stub to a
 * <strong>real SQS queue</strong> on LocalStack, and the unmodified
 * {@link VerificationListener} consumes it.
 *
 * <p>
 * This is the half the conformance suite cannot prove. The TCK shows that the SQS sender
 * and receiver round-trip payloads faithfully; this shows that a consumer can be tested
 * against a producer's contract without the producer running, which is the thing contract
 * testing exists to do.
 *
 * <p>
 * The messaging beans are declared here for the same reason as on the producer side:
 * Stubborn has no SQS autoconfiguration, every backend backs off on
 * {@code @ConditionalOnMissingBean}, and {@link ContractVerifierMessaging} must be
 * declared explicitly or the no-op one is used and the triggered stub goes nowhere.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = { ConsumerApplication.class, VerificationConsumerTest.SqsTestConfiguration.class })
@Import(VerificationConsumerTest.SqsTestConfiguration.class)
@AutoConfigureStubRunner(ids = "com.example:verification-api-producer-sqs:+:stubs", stubsMode = StubsMode.LOCAL)
class VerificationConsumerTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.8");

    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.SQS);

    static {
        LOCALSTACK.start();
    }

    @Autowired
    StubFinder stubFinder;

    @Autowired
    VerificationListener listener;

    @Test
    void publishesTheTriggeredStubToSqsAndTheListenerConsumesIt() {
        this.stubFinder.trigger("verification_published");

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(this.listener.hasReceived("foo")).isTrue());
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

        @Bean
        ContractVerifierMessaging<SqsMessage> contractVerifierMessaging(MessageVerifierSender<SqsMessage> sender,
                MessageVerifierReceiver<SqsMessage> receiver) {
            return new ContractVerifierMessaging<>(sender, receiver);
        }
    }
}
