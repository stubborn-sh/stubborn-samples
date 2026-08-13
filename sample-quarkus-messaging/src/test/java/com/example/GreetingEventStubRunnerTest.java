package com.example;

import java.time.Duration;

import jakarta.inject.Inject;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.quarkus.messaging.MessagingStubRunnerResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots a {@code @QuarkusTest} with the Stubborn Contract <em>messaging</em> stub runner wired in
 * through Quarkus' own {@link QuarkusTestResource} lifecycle. On top of consumer-side HTTP stubbing,
 * {@link MessagingStubRunnerResource} builds a Spring-free Kafka {@code MessageVerifierSender} so a
 * triggered stub's output message is published to a <strong>real broker</strong>.
 *
 * <p>The broker is started automatically by Quarkus Kafka Dev Services on the pinned port
 * {@code 59092} (see {@code application.properties}); the same {@code localhost:59092} is passed to
 * the resource as the {@code brokerAddress} init arg. The producer's output-message contract lives
 * on the test classpath under {@code src/test/resources/contracts/com.example/greeting-events-producer/}
 * ({@code CLASSPATH} mode). Triggering its {@code greeting_event} label sends the contract body to
 * the {@code greetings} topic, where {@link GreetingEventListener} — unchanged from production —
 * consumes it.
 */
@QuarkusTest
@QuarkusTestResource(value = MessagingStubRunnerResource.class,
		initArgs = { @ResourceArg(name = "ids", value = "com.example:greeting-events-producer:+:stubs"),
				@ResourceArg(name = "stubsMode", value = "CLASSPATH"),
				@ResourceArg(name = "transport", value = "kafka"),
				@ResourceArg(name = "brokerAddress", value = "localhost:59092") })
class GreetingEventStubRunnerTest {

	@Inject
	GreetingEventListener listener;

	// Injected by StubRunnerResource#inject — lets a test trigger contract labels programmatically.
	StubFinder stubFinder;

	@Test
	void publishesTheTriggeredStubMessageToTheBroker() {
		this.stubFinder.trigger("greeting_event");

		Awaitility.await()
			.atMost(Duration.ofSeconds(30))
			.untilAsserted(() -> assertThat(this.listener.hasReceived("Hello from the stubbed producer")).isTrue());
	}

}
