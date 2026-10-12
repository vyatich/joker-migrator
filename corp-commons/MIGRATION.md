# corp-commons 1.x → 2.0

2.0 requires Java 17+.

## Automated part

Run the recipe published with this release — it does every rename below and bumps the dependency:

| Recipe | Artifact |
| --- | --- |
| `com.corp.commons.MigrateToCorpCommons2` | `com.corp:corp-commons-recipes:2.0.0` (company repository) |

Run it together with the other recipes of your migration, for example (Gradle, OpenRewrite plugin applied):

```
./gradlew rewriteRun -Drewrite.activeRecipe=com.corp.commons.MigrateToCorpCommons2
```

with `com.corp:corp-commons-recipes:2.0.0` on the `rewrite` classpath.

## All breaking changes

| 1.x | 2.0 | Recipe |
| --- | --- | --- |
| package `com.corp.commons.util` | package `com.corp.commons.core` | yes |
| `CorpLogger` | `CorpLog` (same methods) | yes |
| `CorpContext.getConfig()` | `CorpContext.config()` | yes |
| `new CorpHttpClient(String baseUrl, long timeoutMillis)` | `new CorpHttpClient(String baseUrl, Duration timeout)` | no — by hand |
| `CorpHttpClient.getTimeoutMillis()` returns `long` | `CorpHttpClient.getTimeout()` returns `Duration` | no — by hand |

`CorpConfig` keeps its API; read timeouts with `getLong(...)` and wrap them in `Duration.ofMillis(...)`.
