# Gradle and IntelliJ IDEA baseline

Use the checked-in Gradle **4.10.3** wrapper with a **Java 8 JDK**. The legacy
Liberty plugin uses Gradle APIs removed in later versions. Upgrading the wrapper
alone is not a supported build repair.

## Import

1. Register an installed Java 8 JDK in **File → Project Structure → SDKs** and
   select it as the project SDK. On this workstation it is Zulu Java 8
   (`1.8.0_282`, aarch64).
2. Open this repository's `build.gradle` as a Gradle project, or link it from
   IDEA's Gradle tool window when using a parent workspace. Import the root
   build, which includes the EAR, EJB, web, REST, and Liberty configuration modules.
3. In **Settings → Build, Execution, Deployment → Build Tools → Gradle**, select
   the wrapper distribution, the Java 8 Gradle JVM, and Gradle for build/run.
4. Sync Gradle. Remove any stale link to a previous checkout location.

IDEA 2026.2.3 was verified to import these modules and run delegated compilation
and packaging. It reports that Gradle 4.10.3 support is deprecated. Retain the
wrapper for this baseline; a future IDEA release may require a separate build-tool
upgrade. Gradle 4.10.3 cannot run on this workstation's Java 27 SDK.

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
their Java 7 compilation target; the web and REST modules use the Java 8 build
default. Local `.idea` settings and machine-specific SDK paths are not portable
project files and should remain untracked.

The **DayTrader Build**, **DayTrader Start**, and **DayTrader Stop** configurations
are also available in the parent workspace. [Runtime setup](Restored-runtime.md)
describes the pinned server and database initialization.

The original `build` lifecycle installs/starts/stops Liberty through `fvt`, even
though no functional tests were found. Packaging alone does not verify runtime
behavior. The original configuration module's `clean` task deletes its deployed
applications and Derby data; do not run it against a baseline you want to keep.
