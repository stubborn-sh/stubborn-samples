package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The event model exchanged over the messaging channel.
 *
 * <p>A producer publishes a JSON body such as {@code {"message":"Hello from the stubbed producer"}}
 * to the {@code greetings} topic. In this sample there is no real producer: the Stubborn Contract
 * stub runner replays the producer's published output-message contract onto a real broker when the
 * test triggers its label, and {@link GreetingEventListener} deserializes the payload into this
 * type.
 *
 * @param message the greeting text carried by the event
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GreetingEvent(String message) {
}
