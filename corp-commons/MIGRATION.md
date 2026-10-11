# corp-commons 1.x → 2.0

2.0 requires Java 17+. All breaking changes:

| 1.x | 2.0 |
| --- | --- |
| package `com.corp.commons.util` | package `com.corp.commons.core` |
| `CorpLogger` | `CorpLog` (same methods) |
| `CorpContext.getConfig()` | `CorpContext.config()` |
| `new CorpHttpClient(String baseUrl, long timeoutMillis)` | `new CorpHttpClient(String baseUrl, Duration timeout)` |
| `CorpHttpClient.getTimeoutMillis()` returns `long` | `CorpHttpClient.getTimeout()` returns `Duration` |

`CorpConfig` keeps its API; read timeouts with `getLong(...)` and wrap them in `Duration.ofMillis(...)`.
