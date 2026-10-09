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

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * A Spring-free {@link MessageVerifierSender} for Amazon SQS, built directly on the AWS
 * SDK v2 {@link SqsClient} (no Spring Cloud AWS / {@code SqsTemplate}), so it can back
 * contract verification from any JVM runtime.
 *
 * <p>
 * The contract payload is mapped onto an SQS body and message attributes by
 * {@link SqsWireFormat} — text verbatim, binary Base64-encoded and flagged — so that an
 * ordinary consumer of a JSON queue still reads plain JSON, while non-UTF-8 payloads
 * survive byte-for-byte. See that class for why the distinction is necessary.
 *
 * <p>
 * A {@code destination} is a queue <em>name</em>, resolved to its URL once and cached:
 * SQS requires a URL on every call, and resolving per send would add a network round
 * trip to each one.
 */
public final class StubbornSqsMessageVerifierSender implements MessageVerifierSender<SqsMessage> {

	private static final Logger log = LoggerFactory.getLogger(StubbornSqsMessageVerifierSender.class);

	private final SqsClient client;

	private final SqsQueues queues;

	/**
	 * Creates a sender over the given client.
	 * @param client the SQS client; not closed by this sender, since its lifecycle
	 * belongs to whoever built it
	 */
	public StubbornSqsMessageVerifierSender(SqsClient client) {
		this.client = client;
		this.queues = new SqsQueues(client);
	}

	@Override
	public void send(SqsMessage message, String destination, @Nullable YamlContract contract) {
		send(message.getPayload(), message.getHeaders(), destination, contract);
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode(payload, headers);

		SendMessageRequest.Builder request = SendMessageRequest.builder()
			.queueUrl(this.queues.urlFor(destination))
			.messageBody(encoded.body());

		if (!encoded.attributes().isEmpty()) {
			request.messageAttributes(encoded.attributes()
				.entrySet()
				.stream()
				.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
						(entry) -> MessageAttributeValue.builder()
							.dataType("String")
							.stringValue(entry.getValue())
							.build())));
		}

		this.client.sendMessage(request.build());
		log.debug("Sent a message to SQS queue [{}] with attributes {}", destination, encoded.attributes().keySet());
	}

}
