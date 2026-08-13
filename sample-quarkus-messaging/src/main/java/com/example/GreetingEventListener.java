package com.example;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * A tiny Quarkus messaging consumer bean.
 *
 * <p>It listens on the {@code greetings} channel (mapped to the {@code greetings} Kafka topic in
 * {@code application.properties}) via SmallRye Reactive Messaging's {@link Incoming}. Every message
 * is deserialized from JSON into a {@link GreetingEvent} and recorded in an in-memory holder so a
 * test can await and assert it.
 *
 * <p>In production this listener would consume messages from a real producer; in the test the
 * Stubborn Contract stub runner publishes the producer's contract-defined output message to the
 * same topic when its label is triggered, so the exact same code runs against the producer's
 * published stub.
 */
@ApplicationScoped
public class GreetingEventListener {

	private final ObjectMapper objectMapper;

	private final List<GreetingEvent> received = new CopyOnWriteArrayList<>();

	public GreetingEventListener(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Incoming("greetings")
	public void onGreeting(String payload) throws Exception {
		this.received.add(this.objectMapper.readValue(payload, GreetingEvent.class));
	}

	/**
	 * Returns the greeting events consumed so far.
	 * @return an immutable snapshot of the recorded events
	 */
	public List<GreetingEvent> received() {
		return List.copyOf(this.received);
	}

	/**
	 * Whether an event with the given message has been consumed.
	 * @param message the greeting text to look for
	 * @return {@code true} if such an event was received
	 */
	public boolean hasReceived(String message) {
		return this.received.stream().anyMatch((event) -> message.equals(event.message()));
	}

}
