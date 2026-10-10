# Restored Gradle runtime

The `restore/runtime` branch retains the original application sources, Derby
10.10.1.1, and Java EE 6 server features. It adds Gradle 4.10.3's wrapper and pins
IBM Liberty 24.0.0.9 (`wlp-javaee7` distribution), with the older features installed
separately. The runtime has IBM license terms in `wlp/lafiles`; the sample's Apache
license does not replace those terms. This is a local legacy baseline.

## Build and start

Set `JAVA_HOME` to an installed Java 8 JDK. See [IDEA setup](Using-IDEA.md) for
the project SDK, Gradle JVM, wrapper selection, and packaging configuration.

```bash
./gradlew --no-daemon :daytrader3-ee6:build
./gradlew --no-daemon :daytrader3-ee6:libertyStart
```

The first start installs Liberty into `daytrader3-ee6/build/wlp`, installs the seven
requested features using that runtime's `featureUtility`, deploys the EAR, copies
Derby, and starts `daytrader3_Sample`. Feature signatures are verified by the utility.
Subsequent starts skip installation when all required feature manifests exist.
Logs, messaging state, and transaction logs go to
`daytrader3-ee6/build/liberty-output/daytrader3_Sample/`, keeping the generated
files tracked by upstream untouched. Configuration and the deployed EAR remain
under `daytrader3-ee6-wlpcfg`.
An external `-PlibertyRoot=...` must supply a compatible Liberty installation with
`featureUtility`; the default pinned installation is the verified path.

The inherited full `build` starts and stops the server through `fvt`. No functional
test sources exist, so a successful `build` is not a trading-behavior check. Use
the packaging tasks from the IDEA guide when only building the EAR. In the parent
Migration workspace, local run configurations are **DayTrader Package**,
**DayTrader Build**, **DayTrader Start**, and **DayTrader Stop**.

## Initialize a new local database

1. Open <http://localhost:9083/daytrader> and select **Configuration**.
2. Select **(Re)-create DayTrader Database Tables and Indexes**. This replaces the
   tables in the configured local Derby database.
3. Stop and start the server as directed by the table-creation response.
4. In Configuration, select **Full EJB3** and **Synchronous**. Set Max Users to
   **10** and Max Quotes to **100**, then update the configuration.
5. Select **(Re)-populate DayTrader Database**. This replaces sample users/quotes
   with randomly generated data at the selected size.
6. Stop and start again, then log in as **uid:0** with sample password **xxx**.

The database is in `daytrader3-ee6-wlpcfg/shared/resources/data/tradedb`. Sample
configuration updates reset on server restart; Full EJB3 and synchronous ordering
are also the original defaults. The small fixture size is for functional checking,
not benchmark performance. Keep the original configuration module's `clean` task
away from a database you want to retain: it deletes the database and deployed apps.

```bash
./gradlew --no-daemon :daytrader3-ee6:libertyStop
```

## Verified scope

IDEA 2026.2.3 with Zulu Java 8 imported all modules, compiled, packaged, and ran
the full EAR-module build. Liberty started the EAR with the original features and
both MDB endpoints active. A separate HTTP smoke check in the parent experiment
record passed login, quote lookup, synchronous buy/sell, and portfolio persistence
across fresh logins. Both orders reached `closed`, and the portfolio returned to
its original holding set. This does not establish asynchronous, rollback,
concurrent, REST, JSF, monetary-state, or performance correctness.
