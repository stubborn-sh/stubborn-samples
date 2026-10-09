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

import java.net.URI;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.tck.AbstractMessageVerifierConformanceTests;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Runs the transport-neutral messaging conformance suite against a real SQS, provided by
 * LocalStack.
 *
 * <p>
 * This is the test that actually decides whether the SQS building block behaves like the
 * Kafka, RabbitMQ and JMS ones. {@link SqsWireFormatTests} pins the encoding without a
 * broker; this proves the whole path — attributes really survive the service, long
 * polling really returns the message, and the binary payload really comes back
 * byte-for-byte after a round trip through an XML-safe body.
 *
 * <p>
 * LocalStack is used rather than an in-JVM SQS stand-in so the suite runs against real
 * SQS semantics — visibility timeouts, long polling, message attribute validation.
 *
 * <p>
 * Tagged {@code docker} so it can be excluded where no Docker daemon is available; CI
 * runs it without the exclusion.
 */
@Tag("docker")
class SqsMessageVerifierConformanceTests extends AbstractMessageVerifierConformanceTests<SqsMessage> {

	private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.8");

	private static LocalStackContainer localstack;

	private static SqsClient client;

	private static StubbornSqsMessageVerifierSender sender;

	private static StubbornSqsMessageVerifierReceiver receiver;

	@BeforeAll
	static void startLocalStack() {
		localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(LocalStackContainer.Service.SQS);
		localstack.start();

		client = SqsClient.builder()
			.endpointOverride(URI.create(localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString()))
			.credentialsProvider(StaticCredentialsProvider
				.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
			.region(Region.of(localstack.getRegion()))
			.build();

		sender = new StubbornSqsMessageVerifierSender(client);
		receiver = new StubbornSqsMessageVerifierReceiver(client);
	}

	@AfterAll
	static void stopLocalStack() {
		if (client != null) {
			client.close();
		}
		if (localstack != null) {
			localstack.stop();
		}
	}

	@Override
	protected MessageVerifierSender<SqsMessage> sender() {
		return sender;
	}

	@Override
	protected MessageVerifierReceiver<SqsMessage> receiver() {
		return receiver;
	}

	@Override
	protected SqsMessage message(@Nullable Object payload, Map<String, Object> headers) {
		return new SqsMessage(payload, headers);
	}

}
