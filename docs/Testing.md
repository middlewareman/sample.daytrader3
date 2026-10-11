# Gradle verification on the restored platform

Use the checked-in Gradle 8.14.3 wrapper with Java 8:

```bash
./gradlew --no-daemon --console=plain build
```

The same suite is reached by `:daytrader3-ee6:build` and
`:daytrader3-verification:check`. The explicit `:daytrader3-ee6:fvt` task is an
alias for the verification module. Normal builds package the application and run
an isolated test server; they do not deploy into or stop the interactive server.
Use `:daytrader3-ee6:libertyStart` / `libertyStop` explicitly for interactive use.

The build pins Liberty Gradle plugin 3.10.0 and a Java 8 toolchain. EJB
application classes retain Java 7 bytecode; test classes and the JUnit runner use
Java 8. JUnit Platform launcher 1.10.3 is explicit rather than supplied by Gradle.
IBM Liberty 24.0.0.9 and the Java EE 6 features remain unchanged.

## Test structure and pinned tools

- EJB unit tests: JUnit Jupiter 5.10.3, exercising financial gains and portfolio
  purchase-value calculations, including zero balances and fractional shares.
- `daytrader3-verification`: independent Java 8 HTTP client and JUnit Jupiter
  tests. It has no application/EJB/web module classpath dependency. Jsoup 1.17.2
  parses the legacy HTML; Derby 10.10.1.1 handles fixture loading and stopped-DB
  inspection. JUnit produces Gradle XML and HTML reports.
- Server coverage: JaCoCo 0.8.12, attached to Liberty rather than the HTTP test
  runner. Financial-unit coverage is merged with the server data.

Keep business assertions and test-owned values when migrating. The legacy client,
fixture/SQL adapters, and server lifecycle may need porting; they must not alter
assertion meanings or conceal failures. Test dependencies can remain pinned
independently of the application platform.

## Fixture and lifecycle

All generated test state is under `daytrader3-verification/build/`, including
`liberty-user`, `liberty-output`, the Derby database, and coverage data. The test
server is named `daytrader3_Baseline`. Preparation checks that its configured
ports and server lock are free before replacing the test-owned fixture.

Each suite run recreates 40 synthetic accounts, 40 quotes, and zero initial
orders/holdings. Account `test:n` has cash `10000 + 1000*n`, quote `s:n` has price
`50 + n`, timestamps start at 2020-01-01, and keys start at a fixed seed. The
schema comes from the original Derby DDL. Random sample population is not used.
Trading scenarios use different accounts/quotes so they do not interfere.

The application intentionally randomizes quote prices after trades. Tests verify
the initial fixed purchase price, then capture the current quote *before* selling
and independently calculate execution value and fees. They do not assert identical
random post-trade prices across runs. Fees remain exactly 24.95 for each buy/sell.

The suite starts Liberty, checks readiness, runs HTTP scenarios, stops it,
restarts the same database for durability checks, stops it again, then verifies
cash, orders, holdings, profile changes, and registration directly in Derby.
After restart it reapplies the Full EJB synchronous functional configuration.
DayTrader configuration flags are JVM-local: the default long-run benchmark mode
suppresses account-page order history and must be disabled for these checks.
Reapplying configuration does not reload the fixture or change expected state.
Gradle finalizers stop the test server even after a test-task failure. Mutable
server tests deliberately rerun instead of using up-to-date/build-cache outcomes.
Long performance runs are outside this build.

Default test ports are HTTP 19083, HTTPS 19443, JMS 17276, and JMS SSL 17286.
To avoid a collision, set any of `-PtestHttpPort=...`, `-PtestHttpsPort=...`,
`-PtestJmsPort=...`, and `-PtestJmsSslPort=...`. These changes apply only to the
isolated configuration. Interactive ports and data remain as configured upstream.
If a build is forcibly terminated outside Gradle cleanup, run:

```bash
./gradlew :daytrader3-verification:stopTestServer
```

## Reports and coverage scope

Reports are under:

- `daytrader3-ee6-ejb/build/reports/tests/test/`: calculation unit tests.
- `daytrader3-verification/build/reports/tests/test/`: HTTP cases and named
  Full EJB synchronous, JDBC synchronous, Session-to-JDBC synchronous, and
  Full EJB asynchronous two-phase trading profiles.
- `daytrader3-verification/build/reports/tests/restartTest/`: restart cases.
- `daytrader3-verification/build/reports/tests/persistedTest/`: independent SQL checks.
- `daytrader3-verification/build/reports/coverage/`: JaCoCo HTML report.
- `daytrader3-verification/build/reports/coverage.xml`: detailed JaCoCo XML.
- `daytrader3-verification/build/reports/coverage-summary.json`: source-line and
  source-branch counts by module and key trading classes.

OpenJPA enhances entity bytecode at runtime. Coverage uses the agent's dumped
runtime classes where present and retains all remaining compiled application
classes, including unexercised code. Raw JaCoCo reports include generated
persistence methods; the summary counts only branches attached to source lines.
Keep the class artifacts and instrumentation policy alongside reports when comparing
migrations. Interface and architecture changes can change coverage denominators.

Successful tests establish the named scenarios, not complete equivalence.
An initial restart failure came from the harness omitting that configuration
step. Runtime debugger evidence confirmed deliberate long-run suppression;
restoring the same profile resolved it without changing application code or
weakening the strict order-history and SQL assertions. Cash/holdings and order
history remain independently named restart checks.

Concurrency, broader rollback/failure cases, JMS redelivery/duplicate handling,
REST/address-book assertions, browser/JSF behavior, and performance still need
separate coverage and contracts. Baseline failures and characterization decisions
belong in the superproject experiment record; do not weaken assertions to make
an application migration pass.
