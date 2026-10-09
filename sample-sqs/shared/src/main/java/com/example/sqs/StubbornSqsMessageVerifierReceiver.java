/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.sqs;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * A Spring-free {@link MessageVerifierReceiver} for Amazon SQS, built directly on the AWS
 * SDK v2 {@link SqsClient}.
 *
 * <p>
 * <strong>Determinism.</strong> A contract test asserts on exactly one message and must
 * not flake, which takes more care on SQS than on Kafka:
 *
 * <ul>
 * <li><strong>Long polling, not spinning.</strong> {@code waitTimeSeconds} is set from
 * the caller's timeout (capped at the SQS maximum of 20s) so the call blocks server-side
 * until a message arrives. Short polling would return empty immediately and sample only a
 * subset of hosts, which is the classic source of "the message was there, we just did not
 * see it" flakes.</li>
 * <li><strong>Polling until the deadline.</strong> Because a single long poll can return
 * empty even when a message is in flight, receives are retried until the caller's timeout
 * is spent rather than giving up after one attempt.</li>
 * <li><strong>Delete on receive.</strong> The message is deleted once read, so it cannot
 * reappear after its visibility timeout and leak into a later assertion in the same
 * suite. Contract verification is a consume-once operation; there is no redelivery story
 * to preserve.</li>
 * </ul>
 */
public final class StubbornSqsMessageVerifierReceiver implements MessageVerifierReceiver<SqsMessage> {

	private static final Logger log = LoggerFactory.getLogger(StubbornSqsMessageVerifierReceiver.class);

	/** SQS caps server-side long polling at 20 seconds. */
	private static final int MAX_WAIT_SECONDS = 20;

	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

	private final SqsClient client;

	private final SqsQueues queues;

	/**
	 * Creates a receiver over the given client.
	 * @param client the SQS client; not closed by this receiver, since its lifecycle
	 * belongs to whoever built it
	 */
	public StubbornSqsMessageVerifierReceiver(SqsClient client) {
		this.client = client;
		this.queues = new SqsQueues(client);
	}

	@Override
	public @Nullable SqsMessage receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		String queueUrl = this.queues.urlFor(destination);
		long deadline = System.nanoTime() + timeUnit.toNanos(timeout);

		do {
			long remainingSeconds = Math.max(0, TimeUnit.NANOSECONDS.toSeconds(deadline - System.nanoTime()));
			int waitSeconds = (int) Math.min(MAX_WAIT_SECONDS, Math.max(1, remainingSeconds));

			List<Message> messages = this.client
				.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueUrl)
					.maxNumberOfMessages(1)
					.waitTimeSeconds(waitSeconds)
					.messageAttributeNames("All")
					.build())
				.messages();

			if (!messages.isEmpty()) {
				Message message = messages.get(0);
				this.client.deleteMessage(DeleteMessageRequest.builder()
					.queueUrl(queueUrl)
					.receiptHandle(message.receiptHandle())
					.build());
				return toSqsMessage(message);
			}
		}
		while (System.nanoTime() < deadline);

		log.debug("No message received from SQS queue [{}] within the timeout", destination);
		return null;
	}

	@Override
	public @Nullable SqsMessage receive(String destination, @Nullable YamlContract contract) {
		return receive(destination, DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS, contract);
	}

	private static SqsMessage toSqsMessage(Message message) {
		Map<String, String> attributes = new HashMap<>();
		message.messageAttributes()
			.forEach((name, value) -> attributes.put(name,
					(value.stringValue() != null) ? value.stringValue() : ""));

		SqsWireFormat.Decoded decoded = SqsWireFormat.decode(message.body(), attributes);
		return new SqsMessage(decoded.payload(), decoded.headers());
	}

}
