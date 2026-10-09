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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.messaging.MessagePayloads;

/**
 * Translates between the transport-neutral contract wire form ({@code byte[]} plus
 * headers) and what Amazon SQS can actually carry.
 *
 * <p>
 * This is the one genuinely SQS-specific problem, and it is kept here — pure, with no AWS
 * client in sight — so it can be unit-tested exhaustively without a broker.
 *
 * <p>
 * <strong>Why a translation is needed at all.</strong> Kafka and AMQP carry an opaque
 * {@code byte[]} body, so their building blocks publish contract payloads verbatim. An
 * SQS message body is <em>not</em> a byte array: it is a string, and the API restricts it
 * to characters valid in XML 1.0. Arbitrary bytes therefore cannot be written to a body —
 * {@code 0x00}, {@code 0x80} and {@code 0xFF} are not valid UTF-8 and would be mangled or
 * rejected. A naive {@code new String(bytes, UTF_8)} silently replaces every invalid byte
 * with U+FFFD, so the payload would round-trip as corrupted data rather than fail loudly.
 *
 * <p>
 * <strong>The mapping.</strong> The payload's form is decided once by
 * {@link MessagePayloads} and carried by the {@code contentType} header, exactly as in
 * the Kafka building block:
 *
 * <ul>
 * <li><strong>Text</strong> ({@code contentType} is not binary) — the UTF-8 string goes
 * into the body <em>as-is</em>. An ordinary SQS consumer reading the queue sees plain
 * JSON, not an encoded blob, so the common case stays interoperable with consumers that
 * know nothing about Stubborn.</li>
 * <li><strong>Binary</strong> ({@code contentType} is binary, e.g. Avro or Protobuf) —
 * the bytes are Base64-encoded into the body and flagged with the
 * {@value #CONTENT_TRANSFER_ENCODING_ATTRIBUTE} message attribute, mirroring the MIME
 * convention. The receiver decodes on that flag alone, so byte fidelity is exact.</li>
 * </ul>
 *
 * <p>
 * Encoding only what must be encoded is the deliberate trade: always Base64-ing would be
 * simpler and would break every non-Stubborn consumer of a JSON queue.
 *
 * <p>
 * <strong>Headers.</strong> SQS has no headers; it has message attributes, which are
 * typed and name-restricted (alphanumerics, hyphen, underscore and period only). Headers
 * are carried as {@code String} attributes, and a header whose name SQS would reject is
 * dropped rather than silently corrupting the send — see {@link #isValidAttributeName}.
 */
public final class SqsWireFormat {

	/**
	 * Message attribute that marks a Base64-encoded body, mirroring the MIME
	 * {@code Content-Transfer-Encoding} header.
	 */
	public static final String CONTENT_TRANSFER_ENCODING_ATTRIBUTE = "contentTransferEncoding";

	/** Value of {@value #CONTENT_TRANSFER_ENCODING_ATTRIBUTE} for a Base64 body. */
	public static final String BASE64 = "base64";

	/**
	 * SQS allows at most 10 message attributes per message. Exceeding it is rejected by
	 * the service, so it is worth failing with a comprehensible message instead.
	 */
	public static final int MAX_ATTRIBUTES = 10;

	private SqsWireFormat() {
	}

	/**
	 * Encodes a payload and its headers into an SQS body plus the attributes that must
	 * accompany it.
	 * @param payload the contract payload (may be {@code null})
	 * @param headers the contract headers; a {@code contentType} is added when absent
	 * @return the body and the attributes to send alongside it
	 */
	public static Encoded encode(@Nullable Object payload, @Nullable Map<String, Object> headers) {
		Map<String, Object> effectiveHeaders = new LinkedHashMap<>();
		if (headers != null) {
			effectiveHeaders.putAll(headers);
		}
		// The TCK requires a contentType to be present on the wire even when the
		// contract did not set one, so the receiver can tell text from binary.
		effectiveHeaders.computeIfAbsent(MessagePayloads.CONTENT_TYPE_HEADER,
				(key) -> MessagePayloads.defaultContentType(payload));

		byte[] bytes = MessagePayloads.toByteArray(payload);
		boolean binary = MessagePayloads.isBinaryContentType(effectiveHeaders.get(MessagePayloads.CONTENT_TYPE_HEADER));

		Map<String, String> attributes = new LinkedHashMap<>();
		for (Map.Entry<String, Object> header : effectiveHeaders.entrySet()) {
			if (header.getValue() != null && isValidAttributeName(header.getKey())) {
				attributes.put(header.getKey(), String.valueOf(header.getValue()));
			}
		}

		String body;
		if (binary) {
			body = Base64.getEncoder().encodeToString(bytes);
			attributes.put(CONTENT_TRANSFER_ENCODING_ATTRIBUTE, BASE64);
		}
		else {
			body = new String(bytes, StandardCharsets.UTF_8);
		}

		if (attributes.size() > MAX_ATTRIBUTES) {
			throw new IllegalArgumentException("SQS allows at most " + MAX_ATTRIBUTES
					+ " message attributes, but the contract would send " + attributes.size() + ": "
					+ attributes.keySet());
		}
		return new Encoded(body, attributes);
	}

	/**
	 * Decodes an SQS body and its attributes back into the contract payload and headers.
	 * @param body the SQS message body
	 * @param attributes the SQS message attributes
	 * @return the payload (a {@code String} for text, {@code byte[]} for binary) and
	 * headers
	 */
	public static Decoded decode(String body, @Nullable Map<String, String> attributes) {
		Map<String, Object> headers = new LinkedHashMap<>();
		if (attributes != null) {
			headers.putAll(attributes);
		}
		// Transport detail, not part of the contract: strip it before the payload is
		// compared against the contract's headers.
		boolean base64 = BASE64.equals(headers.remove(CONTENT_TRANSFER_ENCODING_ATTRIBUTE));

		byte[] bytes = base64 ? Base64.getDecoder().decode(body) : body.getBytes(StandardCharsets.UTF_8);
		return new Decoded(MessagePayloads.fromWire(bytes, headers), headers);
	}

	/**
	 * Whether SQS would accept this header name as a message attribute name. SQS permits
	 * alphanumerics, hyphen, underscore and period; a name may not start or end with a
	 * period, nor contain two consecutive periods.
	 * @param name the candidate attribute name
	 * @return {@code true} when SQS would accept it
	 */
	public static boolean isValidAttributeName(@Nullable String name) {
		if (name == null || name.isEmpty() || name.length() > 256) {
			return false;
		}
		if (name.startsWith(".") || name.endsWith(".") || name.contains("..")) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			boolean allowed = Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.';
			if (!allowed) {
				return false;
			}
		}
		return true;
	}

	/** An SQS body together with the message attributes that must accompany it. */
	public record Encoded(String body, Map<String, String> attributes) {
	}

	/** A contract payload and headers reconstructed from an SQS message. */
	public record Decoded(Object payload, Map<String, Object> headers) {
	}

}
