# sample-quarkus

A **Quarkus** consumer that runs Stubborn Contract stubs inside a `@QuarkusTest` — **without
Spring**. It uses `stubborn-contract-stub-runner-quarkus`, which adapts the Spring-free stub
runner core to Quarkus' own `QuarkusTestResourceLifecycleManager` lifecycle.

## What it shows

- `GreetingService` (`src/main/java`) is a plain `@ApplicationScoped` Quarkus bean that calls a
  remote producer over HTTP. Its base URL comes from MicroProfile Config
  (`stubborn.contract.stubrunner.runningstubs.greeting-producer.url`).
- `GreetingServiceStubRunnerTest` (`src/test/java`) registers `StubRunnerResource` with
  `@QuarkusTestResource`. Before the test runs, the adapter starts the producer's stubs on a free
  port and publishes that port/URL as Quarkus config — so the unchanged `GreetingService` talks to
  the stub.
- The producer's stubs live on the test classpath under
  `src/test/resources/mappings/com.example/greeting-producer/mappings/` (`CLASSPATH` mode). In a
  real project these come from the producer's published `...-stubs.jar`; here a single WireMock
  mapping keeps the sample self-contained.

## Run it

```bash
./mvnw clean test
```

## Notes

- **Java 21+** works out of the box; the sample targets Java 17 bytecode (`maven.compiler.release`).
- The Stubborn Contract adapter is pinned by version rather than via the
  `stubborn-contract-dependencies` BOM. That BOM also manages shared transitive libraries (netty,
  jackson, tomcat) that the Quarkus platform BOM already manages for its own tested versions;
  importing both would let one override the other. Pinning just the adapter keeps the Quarkus BOM
  authoritative.
- Producer-side contract *verification* needs no Quarkus-specific module: generate the verifier
  tests in `EXPLICIT` mode and drive a running `@QuarkusTest` endpoint with RestAssured. See
  `docs/integrations/quarkus.md` in the `stubborn-contract` repository.
