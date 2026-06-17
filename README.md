# ProcessLog

> **Archived.** This project has been superseded by
> [apache-log-management](../apache-log-management), which merges this codebase
> with the lead-manager project into a single, cleaner tool called LogPipe.
> All features developed here were ported to that project before archival.

A Java application for reading Apache log files and loading them into a database schema.

Hung off the central table (`ApacheLogs`) are a series of `ignore*` and `delete*` tables.

- If a row matches a **delete** table it is never stored in the database.
- If a row matches an **ignore** table it is stored but flagged with a non-zero `ignoreReason`.

It is common to delete the developer's own IP address and paths like `/wp-content` (image
downloads we don't care about), and to ignore traffic from cloud crawlers such as AWS and
Azure which tend to be indexing or security-scanning tools.

## Project Structure

```
process-logs/
├── pom.xml                    # Maven build configuration
├── get-logs.sh                # Main CLI wrapper (symlink from ~/bin)
├── run.sh                     # Low-level JAR launcher
├── src/
│   ├── main/
│   │   ├── java/             # Application source code
│   │   │   └── com/s4apps/processlog/
│   │   └── resources/        # secrets.properties, version.properties, logging config
│   └── test/
│       └── java/             # JUnit 5 tests
└── target/                    # Build output (generated, not committed)
```

## Building

Build the shaded JAR (skipping tests):

```bash
mvn -q -DskipTests package
```

`run.sh` checks whether any source file is newer than the JAR and **exits with an error**
if a rebuild is needed — this prevents accidentally running stale code.

## Running

All normal operations go through `get-logs.sh`:

```bash
get-logs.sh --help
```

| Option | Description |
|---|---|
| `--download` | Download log files from the server |
| `--import` | Parse log files and load into the database |
| `--rebuild` | Re-evaluate all ignore/delete rules against stored rows |
| `--check` | Validate database integrity |
| `--delete-old` | Purge rows older than 180 days |
| `--version` | Print the JAR version and build timestamp |
| `--quiet` | Suppress all output |
| `--log` | Append output to a dated log file |

The default (no arguments) runs `--download --import --delete-old`.

`get-logs.sh` checks for a stale JAR before doing any work and exits with a clear error
message if the JAR is out of date.

## Dependencies

- **Jakarta Persistence API 3.1.0** — JPA interfaces
- **Hibernate ORM 6.4.4.Final** — JPA implementation
- **Hibernate HikariCP 6.4.4.Final** — connection pooling
- **Apache Commons CLI 1.11.0** — command-line argument parsing
- **Apache Commons Lang3 3.20.0** — utility functions
- **MySQL Connector/J 9.6.0** — MySQL database connectivity
- **SLF4J Simple 2.0.13** — logging

## Database Secrets

Database credentials are loaded from `src/main/resources/secrets.properties` at runtime.

```properties
db.url=jdbc:mysql://localhost:3306/processlog
db.user=processlog
db.password=change-me
```

## Module Overview

### `ProcessLog.java`
Main import command. Reads Apache combined-format log files, parses each line, applies
delete/ignore rules, and writes rows to the database.

### `Rebuild.java`
Re-evaluates every stored row against the current ignore/delete rules without re-importing
the source files. Run this after changing any rule table.

### `Check.java`
Validates database integrity:
1. Counts rows that match a delete rule but were not deleted.
2. Verifies each ignore boolean column matches the corresponding rule table.
3. Verifies the `ignoreReason` bitmask is consistent with the individual boolean flags.

### `DeleteOld.java`
Purges rows older than 180 days.

### `Version.java`
Prints the version and build timestamp embedded in the JAR at compile time.

## IgnoreReason Flags

`ignoreReason` is a bitmask stored on each row. A value of `0` means the row is not
ignored. Non-zero values are produced by OR-ing one or more of the constants below.
Each flag also has a dedicated boolean column for easier querying.

| Constant | Value | Boolean column | Meaning |
|---|---|---|---|
| `REASON_UNKNOWN` | 1 | — | Catch-all / unclassified |
| `REASON_IP` | 2 | `ignoreIp` | IP matched an `IgnoreIp` rule |
| `REASON_URL` | 4 | `ignoreUrl` | URL prefix matched an `IgnoreUrl` rule |
| `REASON_SERVER` | 8 | `ignoreServer` | Server/referer matched an `IgnoreServer` rule |
| `REASON_METHOD` | 16 | `ignoreMethod` | HTTP method matched an `IgnoreMethod` rule |
| `REASON_BOT` | 32 | `ignoreBot` | User-agent identified as a bot |

### Bot Detection

Bot detection is code-only (no database rule table). `RowStringStorage.ignoreBot()`
returns `true` if the user-agent string contains `bot`, `crawler`, or `spider`
(case-insensitive). This catches the most common web crawlers (Googlebot, YandexBot,
GPTBot, Bingbot, etc.).

After adding the `ignoreBot` column or changing bot detection logic, run
`get-logs.sh --rebuild` to backfill all existing rows.
