# Java 21 / Jakarta EE 11 branch

This experiment branch migrates the repaired DayTrader EAR to Open Liberty
26.0.0.9 with `jakartaee-11.0`, Jakarta Persistence 3.2 (default EclipseLink),
Faces 4.1 and XML Binding 4.0. Gradle 8.14.3 and Liberty plugin 3.10.0 remain
pinned. The existing EAR/module names, HTML trading interface, Derby 10.10.1.1
schema, embedded JMS and synchronous/JDBC/asynchronous profiles are retained.

## Build and verification

Install Java 21 and Java 8. Application compilation and unit tests use Java 21;
the independent HTTP/fixture/SQL module retains its Java 8 toolchain. Select
Java 21 as `JAVA_HOME` and the IDEA Gradle JVM. If a JDK lives outside Gradle's
automatic discovery locations, register its path with the Gradle project
property `-Porg.gradle.java.installations.paths=/path/to/jdk` (comma-separated
for multiple paths). A `-D` JVM property does not register this toolchain.

```bash
./gradlew --no-daemon --console=plain build
```

The build installs the pinned Open Liberty archive in
`daytrader3-ee6/build/jakarta-runtime/wlp`, separate from the old runtime path.
An existing target installation can be selected with
`-PdaytraderLibertyHome=/path/to/wlp`; its installation is skipped, and required
features are checked. Do not use `libertyRoot`: it conflicts with plugin state.
Validate the selected runtime's version separately when supplying your own path.

All 52 frozen cases pass in the recorded shell runs: 8 unit, 19 HTTP, 14 restart,
11 SQL. This includes the original 49 cases and three JSF/REST checks established
before transformation. Test source and monetary assertions are unchanged.
The isolated server/database remain under `daytrader3-verification/build`.
IDEA also passes the 44 HTTP/restart/SQL cases with unit results up to date.
The recorded run uses a Java 8 Gradle JVM and an explicit Java 21 toolchain
path; selecting Java 21 as IDEA's Gradle JVM remains a setup recommendation.
Successful builds and recorded failed test attempts stop the server automatically.
See [Testing](Testing.md) for fixture semantics and limitations.

To recompile without removing interactive data:

```bash
./gradlew --no-daemon :daytrader3-ee6-ejb:clean :daytrader3-ee6-web:clean \
  :daytrader3-ee6-rest:clean :daytrader3-verification:clean build
```

The legacy configuration module's `clean` task deletes interactive data; avoid
it when preserving that database. Interactive migration/startup is not established
by the isolated suite. The historical restoration guides describe the old branch.
The legacy Travis configuration has not been validated for this two-JDK setup.

## Compatibility decisions

- Enterprise imports/descriptors move to Jakarta; Java SE naming, SQL and XML stay.
- Faces managed beans become CDI `@Named` / `@RequestScoped` beans.
- Container APIs are provided dependencies and are not bundled in the EAR.
- Pages 4 removes `isThreadSafe`; redundant `true` declarations are removed.
- The account servlet accepts `Collection`, as declared by the service, rather
  than casting EclipseLink's returned collection to `ArrayList`.
- Shared caching is explicitly disabled. EclipseLink otherwise retained stale
  inverse order collections; JDBC also writes the same schema outside JPA.
- The REST single-address GET explicitly retains the legacy HTTP 415 response
  when Content-Type is absent. Its separate XML media-type defect remains open.
- Full EE 11 startup requires a registry and keystore/LTPA settings. Checked-in
  passwords are synthetic local-sample defaults; the registry has no users.

The superproject migration record preserves target checksums, failed attempts,
debugger observations, test reports, workload comparisons and known gaps. Passing
these scenarios demonstrates the bounded contract, not universal equivalence.
All six matching workload fixtures pass financial checks. Median p95 response
time exceeds the predefined local investigation threshold at both worker counts;
steady-state performance acceptance remains open.
