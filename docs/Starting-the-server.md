# Managing the server with Gradle

Use Java 8 and the checked-in Gradle wrapper:

```bash
./gradlew --no-daemon :daytrader3-ee6:libertyStart
./gradlew --no-daemon :daytrader3-ee6:libertyStop
```

The build pins IBM Liberty 24.0.0.9 and installs the original Java EE 6 features.
See [runtime and database setup](Restored-runtime.md) for first-start instructions.

Server output goes to `daytrader3-ee6/build/liberty-output/daytrader3_Sample/`.
The full `build` runs the [verification suite](Testing.md) against an isolated
server and database, leaving the interactive server intact. Do not run the server-configuration
project's `clean` task against data you intend to retain; it deletes local database
and deployed application data.
