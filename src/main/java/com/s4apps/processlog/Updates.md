# Updates — Bot Detection

## Summary

Bots appear in the Apache log data and need to be optionally excluded from analytics.

## Implementation (completed 2026-06-17)

All three items below were implemented and ported to the successor project
(`apache-log-management`) before this project was archived.

### Changes made

- `ToolsAndConstants.java` — added `REASON_BOT = 32`
- `RowStringStorage.java` — added `ignoreBot()` method; updated `getIgnoreReason()` to OR in `REASON_BOT`
- `model/ApacheLog.java` — added `ignoreBot` boolean field, getter, setter
- `JpaAccess.java` — updated `applyFlags()`, added `countIgnoreBotFlagged()`, updated `countIgnoreReasonMismatch()` bitmask formula
- `Check.java` — prints bot-flagged row count
- Database — `ALTER TABLE ApacheLogs ADD COLUMN ignoreBot TINYINT(1) NOT NULL DEFAULT 0`

### Bot detection logic (Q2 & Q3)

Detection is code-only in `RowStringStorage.ignoreBot()`. The user-agent string is
checked (case-insensitive) for the substrings `bot`, `crawler`, and `spider`. This
covers the most common crawlers seen in the log data:

- `Googlebot`, `Bingbot`, `YandexBot`, `DuckDuckBot`
- `GPTBot`, `ChatGPT-User`, `ClaudeBot`, `OAI-SearchBot`
- `LinkupBot`, `AhrefsBot`, `SemrushBot`, `Baiduspider`
- `meta-externalagent` (Facebook crawler — matched via `crawler` in URL)

Additional detection strategies not yet implemented (future work in apache-log-management):
- Detect headless browsers by user-agent pattern (e.g. `HeadlessChrome`)
- Flag requests with no referer + non-browser accept headers
- Flag IPs that make an unusually high number of requests in a short window
