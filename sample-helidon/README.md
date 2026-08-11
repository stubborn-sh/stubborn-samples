# sample-helidon

A **Helidon MicroProfile** consumer that runs Stubborn Contract stubs via `@HelidonTest` —
**without Spring**.

> **0.1.0 note:** the Helidon adapter (a `@StubRunner` annotation + a JUnit 5 extension) is
> **embedded in this sample** under `src/test/java/com/example/stubrunner`, layered over the
> published Spring-free `stubborn-contract-stub-runner` core. It is shipped as copy-pasteable sample
> code rather than a published module for 0.1.0; if there's demand it can be promoted to a
> first-class `stubborn-contract-stub-runner-helidon` artifact. The extension publishes running stub
> ports as MicroProfile Config.

## What it shows

- `GreetingService` (`src/main/java`) is a plain `@ApplicationScoped` CDI bean that calls a remote
  producer over HTTP. Its producer port is injected from MicroProfile Config
  (`stubborn.contract.stubrunner.runningstubs.greeting-producer.port`).
- `GreetingServiceStubRunnerTest` (`src/test/java`) combines `@StubRunner` and `@HelidonTest`.
  `@StubRunner` starts the producer's stubs on a free port and publishes that port as a System
  property (which Helidon MP Config reads by default); `@HelidonTest` then boots the CDI container,
  and the injected `GreetingService` — unchanged from production — talks to the stub.
- The stubs live on the test classpath under
  `src/test/resources/META-INF/com.example/greeting-producer/1.0.0/mappings/` (`CLASSPATH` mode).

## Annotation order matters

```java
@StubRunner(ids = "com.example:greeting-producer:+:stubs", stubsMode = StubsMode.CLASSPATH) // first
@HelidonTest                                                                                // second
class GreetingServiceStubRunnerTest { ... }
```

`@StubRunner` must precede `@HelidonTest` so its `beforeAll` sets the stub-port System property
*before* Helidon snapshots its MicroProfile Config. Reversing the order can leave the
`@ConfigProperty` lookup unresolved.

## Run it

```bash
./mvnw clean test
```

## Notes

- **Java 21+ is required** — all of Helidon 4.x needs Java 21 (virtual threads). This sample targets
  Java 21 bytecode.
- **Jetty is pinned to 12.0.37** (the version the stub runner's embedded WireMock is tested
  against). Jetty is only present transitively via WireMock (test scope) — Helidon runs on its own
  WebServer — so pinning it is safe.
- The adapter is pinned by version rather than via the `stubborn-contract-dependencies` BOM, so the
  Helidon BOM stays authoritative for shared transitives.
- You can also inject `StubFinder` straight into a test method (as the second test here does) to
  resolve stub URLs programmatically — no CDI required.
- Producer-side contract *verification* needs no Helidon-specific module: generate the verifier
  tests in `EXPLICIT` mode and drive a running `@HelidonTest` server with RestAssured. See
  `docs/integrations/helidon.md` in the `stubborn-contract` repository.
