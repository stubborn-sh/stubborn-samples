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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the SQS wire mapping.
 *
 * <p>
 * These deliberately need no broker. The conformance suite proves the mapping against a
 * real LocalStack SQS, but the interesting failure — byte fidelity through an XML-safe
 * string body — is pure logic, and pinning it here means a conformance failure points at
 * the client wiring rather than at the encoding.
 */
class SqsWireFormatTests {

	/**
	 * The exact payload the TCK sends. None of {@code 0x00}, {@code 0xFF}, {@code 0xFE}
	 * or {@code 0x80} is valid UTF-8, which is precisely why an SQS body cannot carry it
	 * raw.
	 */
	private static final byte[] TCK_BINARY_PAYLOAD = { 0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, (byte) 0x80, 'h',
			'i' };

	@Test
	@DisplayName("a text payload goes into the body as-is, so plain SQS consumers still read JSON")
	void text_payload_is_not_encoded() {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode("{\"bookName\":\"foo\"}",
				Map.of("contentType", "application/json"));

		assertThat(encoded.body()).isEqualTo("{\"bookName\":\"foo\"}");
		assertThat(encoded.attributes()).containsEntry("contentType", "application/json")
			.doesNotContainKey(SqsWireFormat.CONTENT_TRANSFER_ENCODING_ATTRIBUTE);
	}

	@Test
	@DisplayName("a binary payload is Base64-encoded and flagged")
	void binary_payload_is_base64_encoded_and_flagged() {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode(TCK_BINARY_PAYLOAD,
				Map.of("contentType", "application/octet-stream"));

		assertThat(encoded.body()).isEqualTo("AAEC//6AaGk=");
		assertThat(encoded.attributes())
			.containsEntry(SqsWireFormat.CONTENT_TRANSFER_ENCODING_ATTRIBUTE, SqsWireFormat.BASE64);
	}

	@Test
	@DisplayName("binary bytes survive a round-trip exactly — the whole point of the encoding")
	void binary_payload_round_trips_byte_for_byte() {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode(TCK_BINARY_PAYLOAD,
				Map.of("contentType", "application/octet-stream"));
		SqsWireFormat.Decoded decoded = SqsWireFormat.decode(encoded.body(), encoded.attributes());

		assertThat(decoded.payload()).isInstanceOf(byte[].class);
		assertThat((byte[]) decoded.payload()).containsExactly(TCK_BINARY_PAYLOAD);
	}

	@Test
	@DisplayName("writing raw bytes into a String body would corrupt them — guards the design choice")
	void naive_string_conversion_would_corrupt_the_payload() {
		byte[] mangled = new String(TCK_BINARY_PAYLOAD, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);

		assertThat(mangled).isNotEqualTo(TCK_BINARY_PAYLOAD);
	}

	@Test
	@DisplayName("a text payload round-trips as a String, not as bytes")
	void text_payload_round_trips_as_text() {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode("{\"bookName\":\"foo\"}",
				Map.of("contentType", "application/json"));
		SqsWireFormat.Decoded decoded = SqsWireFormat.decode(encoded.body(), encoded.attributes());

		assertThat(decoded.payload()).isEqualTo("{\"bookName\":\"foo\"}");
		assertThat(decoded.headers()).containsEntry("contentType", "application/json");
	}

	@Test
	@DisplayName("non-ASCII text survives without Base64")
	void utf8_text_round_trips() {
		String payload = "{\"name\":\"Łukasz — Grzejszczak ✅\"}";

		SqsWireFormat.Encoded encoded = SqsWireFormat.encode(payload, Map.of("contentType", "application/json"));
		SqsWireFormat.Decoded decoded = SqsWireFormat.decode(encoded.body(), encoded.attributes());

		assertThat(encoded.attributes()).doesNotContainKey(SqsWireFormat.CONTENT_TRANSFER_ENCODING_ATTRIBUTE);
		assertThat(decoded.payload()).isEqualTo(payload);
	}

	@Test
	@DisplayName("a contentType is added when the contract omits one (TCK: adds_default_content_type_when_absent)")
	void default_content_type_is_added_when_absent() {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode("{\"a\":1}", Map.of());

		assertThat(encoded.attributes()).containsKey("contentType");
	}

	@Test
	@DisplayName("the transfer-encoding flag is transport detail and never leaks into contract headers")
	void transfer_encoding_is_stripped_from_decoded_headers() {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode(TCK_BINARY_PAYLOAD,
				Map.of("contentType", "application/octet-stream"));
		SqsWireFormat.Decoded decoded = SqsWireFormat.decode(encoded.body(), encoded.attributes());

		assertThat(decoded.headers()).doesNotContainKey(SqsWireFormat.CONTENT_TRANSFER_ENCODING_ATTRIBUTE)
			.containsEntry("contentType", "application/octet-stream");
	}

	@Test
	@DisplayName("headers are carried as attributes and come back intact")
	void headers_round_trip() {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("contentType", "application/json");
		headers.put("my-header", "my-value");
		headers.put("trace_id", "abc123");

		SqsWireFormat.Decoded decoded = roundTrip("{}", headers);

		assertThat(decoded.headers()).containsEntry("my-header", "my-value").containsEntry("trace_id", "abc123");
	}

	@ParameterizedTest
	@ValueSource(strings = { "contentType", "my-header", "my_header", "my.header", "a1" })
	void accepts_attribute_names_sqs_allows(String name) {
		assertThat(SqsWireFormat.isValidAttributeName(name)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { ".leading", "trailing.", "two..dots", "has space", "has:colon", "" })
	void rejects_attribute_names_sqs_forbids(String name) {
		assertThat(SqsWireFormat.isValidAttributeName(name)).isFalse();
	}

	@Test
	@DisplayName("a header SQS would reject is dropped rather than failing the send")
	void invalid_attribute_names_are_dropped() {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("contentType", "application/json");
		headers.put("not a valid name", "value");

		SqsWireFormat.Encoded encoded = SqsWireFormat.encode("{}", headers);

		assertThat(encoded.attributes()).containsKey("contentType").doesNotContainKey("not a valid name");
	}

	@Test
	@DisplayName("exceeding the 10-attribute SQS limit fails with a comprehensible message")
	void too_many_attributes_is_rejected_up_front() {
		Map<String, Object> headers = new LinkedHashMap<>();
		for (int i = 0; i < SqsWireFormat.MAX_ATTRIBUTES + 1; i++) {
			headers.put("header" + i, "value" + i);
		}

		assertThatIllegalArgumentException().isThrownBy(() -> SqsWireFormat.encode("{}", headers))
			.withMessageContaining("at most " + SqsWireFormat.MAX_ATTRIBUTES);
	}

	@Test
	@DisplayName("a null payload is carried as an empty body")
	void null_payload_is_an_empty_body() {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode(null, Map.of("contentType", "application/json"));

		assertThat(encoded.body()).isEmpty();
	}

	private static SqsWireFormat.Decoded roundTrip(Object payload, Map<String, Object> headers) {
		SqsWireFormat.Encoded encoded = SqsWireFormat.encode(payload, headers);
		return SqsWireFormat.decode(encoded.body(), encoded.attributes());
	}

}
