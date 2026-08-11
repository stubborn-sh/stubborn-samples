# sample-micronaut

A **Micronaut** consumer that runs Stubborn Contract stubs via a `@MicronautTest` — **without
Spring**. It uses `stubborn-contract-stub-runner-micronaut`, which plugs the Spring-free stub
runner core into Micronaut's `TestPropertyProvider` seam.

## What it shows

- `GreetingClient` (`src/main/java`) is a Micronaut declarative `@Client` whose base URL is a
  property placeholder: `${stubborn.contract.stubrunner.runningstubs.greeting-producer.url}`.
- `GreetingClientTest` (`src/test/java`) is a `@MicronautTest` that **extends `StubRunnerTest`**.
  Because `StubRunnerTest` implements `TestPropertyProvider`, the producer's stubs start and their
  URL is published as configuration *before* the application context comes up — so the injected
  `@Client` already points at the running stub.
- The stubs live on the test classpath under
  `src/test/resources/META-INF/com.example/greeting-producer/1.0.0/mappings/` (`CLASSPATH` mode). In
  a real project these come from the producer's published `...-stubs.jar`.

## Run it

```bash
./mvnw clean test
```

## Notes

- **Java 21+** works out of the box; the sample targets Java 17 bytecode. (Micronaut 4.x supports
  Java 17/21. Micronaut 5.0 raised the baseline to Java 25, which is why the integration and this
  sample stay on the latest 4.x line.)
- **Jetty is pinned to 12.0.37** (the version the stub runner's embedded WireMock is tested
  against). The Micronaut platform BOM otherwise forces core `jetty-*` to a newer line while
  WireMock's `jetty-ee10-*` artifacts stay behind, and the mismatched pair throws
  `NoSuchMethodError` when the stub server starts. Jetty is only present transitively via WireMock
  (test scope) — Micronaut runs on Netty — so pinning it is safe.
- The adapter is pinned by version rather than via the `stubborn-contract-dependencies` BOM, so the
  Micronaut platform BOM stays authoritative for shared transitives (netty, jackson).
- Producer-side contract *verification* needs no Micronaut-specific module: generate the verifier
  tests in `EXPLICIT` mode and drive a running `@MicronautTest` server with RestAssured. See
  `docs/integrations/micronaut.md` in the `stubborn-contract` repository.
