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
import java.util.concurrent.ConcurrentHashMap;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * Resolves a queue <em>name</em> — which is what a contract's {@code sentTo} carries — to
 * the queue URL every SQS call requires, caching the result.
 *
 * <p>
 * Resolution is cached because {@code GetQueueUrl} is a network round trip, and a
 * contract test that sends in a loop would otherwise pay for it on every message.
 *
 * <p>
 * A missing queue is created rather than failing. That is the right default for contract
 * verification, where the queue is test scaffolding rather than production topology: a
 * generated test names a destination and expects to be able to use it, the same way the
 * Kafka building block relies on topic auto-creation.
 */
final class SqsQueues {

	private final SqsClient client;

	private final Map<String, String> urlsByName = new ConcurrentHashMap<>();

	SqsQueues(SqsClient client) {
		this.client = client;
	}

	/**
	 * Returns the URL of the named queue, creating the queue if it does not exist.
	 * @param queueName the queue name, as carried by a contract's destination
	 * @return the queue URL
	 */
	String urlFor(String queueName) {
		return this.urlsByName.computeIfAbsent(queueName, this::resolveOrCreate);
	}

	private String resolveOrCreate(String queueName) {
		try {
			return this.client.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
		}
		catch (QueueDoesNotExistException ex) {
			return this.client.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();
		}
	}

}
