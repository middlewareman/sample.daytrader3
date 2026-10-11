# Building and running with Gradle

Use the checked-in **Gradle 8.14.3 wrapper with Java 8**. Gradle is the sole
supported build for this fork; Maven project descriptors have been removed.
See [IDEA setup](Using-IDEA.md) or [Eclipse/WDT](Using-WDT.md) for IDE use.

## Obtain the fork

```bash
git clone https://github.com/middlewareman/sample.daytrader3.git
cd sample.daytrader3
git switch tooling/baseline-tests
```

This branch contains the restored runtime and baseline verification suite.
In the Migration superproject, use its selected subject revision.
The original upstream source and runtime-restoration revisions remain in Git history.

## Package the application

Set `JAVA_HOME` to your installed Java 8 JDK, then run:

```bash
./gradlew --no-daemon --console=plain :daytrader3-ee6:ear :daytrader3-ee6:copyDerbyLib
```

The EAR is produced at `daytrader3-ee6/build/libs/daytrader3-ee6.ear`. Derby
libraries are copied to the server's shared resources. Explicit `libertyStart`
deploys the EAR to the interactive server. Application sources and Java EE 6
features are retained.

## Run the full build

```bash
./gradlew --no-daemon --console=plain build
```

The [verification suite](Testing.md) starts and stops an isolated Liberty server,
loads a deterministic fixture, runs named tests, and reports application coverage.
The server-configuration project's `clean` task deletes local database and
application data; use it only when that reset is intended.

## Run the application

Follow [the pinned runtime and database setup](Restored-runtime.md), including
feature installation and explicit initialization of a new database.

```bash
./gradlew --no-daemon :daytrader3-ee6:libertyStart
# Open http://localhost:9083/daytrader
./gradlew --no-daemon :daytrader3-ee6:libertyStop
```

Logs and generated server state are under
`daytrader3-ee6/build/liberty-output/daytrader3_Sample/`.
