# Gradle and IntelliJ IDEA baseline

Use the checked-in Gradle **8.14.3** wrapper with a **Java 8 JDK**. The
Liberty plugin is pinned to **3.10.0**. The Java 8 toolchain fixes the compiler
and test runner; isolated Liberty tests also launch that JVM. Keep the Gradle JVM
on Java 8 for the verified setup and interactive server commands.

## Import

1. Register an installed Java 8 JDK in **File → Project Structure → SDKs** and
   select it as the project SDK. On this workstation it is Zulu Java 8
   (`1.8.0_504`, aarch64, in the current tooling validation).
2. Open this repository's `build.gradle` as a Gradle project, or link it from
   IDEA's Gradle tool window when using a parent workspace. Import the root
   build, which includes the EAR, EJB, web, REST, and Liberty configuration modules.
3. In **Settings → Build, Execution, Deployment → Build Tools → Gradle**, select
   the wrapper distribution, the Java 8 Gradle JVM, and Gradle for build/run.
4. Sync Gradle. Remove any stale link to a previous checkout location.

IDEA 2026.2.3 imported and built the earlier Gradle 4.10.3 restoration.
The upgraded build and concurrent account repair have now passed the parent
workspace’s **DayTrader Build** configuration, including isolated HTTP/restart/SQL
checks and server shutdown. Compilation was up-to-date in that IDEA run; clean
compilation was verified separately through the shell. Explicit IDEA reimport and
interactive startup remain unverified. Apply the project code style
before committing changed code. Gradle 8.14.3 supports running on Java 8, but
Gradle 9 requires a newer daemon JVM and a separate Liberty plugin upgrade.

## Build without starting a server

Create a Gradle run configuration pointing to the repository root with tasks:

```text
:daytrader3-ee6:ear :daytrader3-ee6:copyDerbyLib
```

The parent Migration workspace has a local **DayTrader Package** configuration
with these tasks. From a shell, set `JAVA_HOME` to your Java 8 JDK and run:

```bash
./gradlew --no-daemon --console=plain :daytrader3-ee6:ear :daytrader3-ee6:copyDerbyLib
```

The EAR is `daytrader3-ee6/build/libs/daytrader3-ee6.ear`. EJB sources retain
their Java 7 compilation target; the web and REST modules explicitly retain Java 8 bytecode. Local `.idea` settings and machine-specific SDK paths are not portable
project files and should remain untracked.

The **DayTrader Build**, **DayTrader Start**, and **DayTrader Stop** configurations
are also available in the parent workspace. [Runtime setup](Restored-runtime.md)
describes the pinned server and database initialization.

The current `build` lifecycle runs the isolated HTTP/restart/SQL suite and
coverage through `fvt`; it leaves the interactive server alone. The original
configuration module's `clean` task deletes deployed applications and Derby data;
do not run it against a baseline you want to keep. To force application/test
recompilation without deleting the interactive database or installed runtime, use:

```bash
./gradlew --no-daemon :daytrader3-ee6-ejb:clean :daytrader3-ee6-web:clean \
  :daytrader3-ee6-rest:clean :daytrader3-verification:clean build
```
