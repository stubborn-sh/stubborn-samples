# sample-quarkus-messaging

A **Quarkus** messaging consumer that triggers a Stubborn Contract stub onto a **real Kafka broker**
inside a `@QuarkusTest` — **without Spring**. It uses
`stubborn-contract-stub-runner-messaging-quarkus`, which extends the Spring-free Quarkus stub-runner
adapter with a real `MessageVerifierSender`, wired through Quarkus'
`QuarkusTestResourceLifecycleManager` lifecycle.

## What it shows

- `GreetingEventListener` (`src/main/java`) is a plain `@ApplicationScoped` Quarkus bean that
  consumes messages from the `greetings` topic via SmallRye Reactive Messaging
  (`@Incoming("greetings")`) and records each `GreetingEvent` in an in-memory holder.
- `GreetingEventStubRunnerTest` (`src/test/java`) registers `MessagingStubRunnerResource` with
  `@QuarkusTestResource`. On top of the stub coordinates it passes `transport=kafka` and
  `brokerAddress=localhost:59092`, so the adapter builds a Spring-free Kafka sender. Triggering the
  contract's `greeting_event` label publishes the contract body to the `greetings` topic; the
  unchanged listener then consumes it.
- The broker is started automatically by **Quarkus Kafka Dev Services** — no manual Testcontainers
  wiring. The Dev Services port is pinned to `59092` in `application.properties` so the test can pass
  the very same address to `brokerAddress`.
- The producer's output-message contract lives on the test classpath under
  `src/test/resources/contracts/com.example/greeting-events-producer/` (`CLASSPATH` mode). In a real
  project this comes from the producer's published `...-stubs.jar`; here a single YAML contract keeps
  the sample self-contained.

## Run it

```bash
./mvnw clean test
```

Requires a container runtime (Docker/Podman) for Kafka Dev Services.

## Notes

- **Java 21+** works out of the box; the sample targets Java 17 bytecode (`maven.compiler.release`).
- The Stubborn Contract adapter is pinned by version rather than via the
  `stubborn-contract-dependencies` BOM. That BOM also manages shared transitive libraries (netty,
  jackson, tomcat) that the Quarkus platform BOM already manages for its own tested versions;
  importing both would let one override the other. Pinning just the adapter keeps the Quarkus BOM
  authoritative.
- The stub runner builds the Kafka sender in its test-resource `start()` (before the application
  boots), but only *sends* when the test calls `trigger(...)` — by then Dev Services has brought the
  broker up on the pinned port, so the lazily-connecting Kafka producer reaches it.
- Producer-side contract *verification* needs no Quarkus-specific module: generate the verifier tests
  and drive the producer's messaging. See `docs/integrations/quarkus.md` in the `stubborn-contract`
  repository.
