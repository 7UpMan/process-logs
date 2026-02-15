# ProcessLog

A Java application for reading ApacheLog files and loading them into a database schema.

Hung off the central table called (ApacheLogs) are a series of ignore* and delete* tables.

If things are found in the delete* tables then if the source data matches an item
it is never loaded into the detabase, i.e. it is deleted.

If it matches a ignore* item, then it is loaded into the database and the record
is tagges as ignored with the ignoreReason column.

It is common to "delete" things like the IP address of the developer and also
to delete everyting under wp-content (because we don't care about image file
downloads).  I tend to ignore any traffic from AWS, Azure etc. at it tends
to be tools verifying that the pages are not harmful or search engines building
their indicies.

## Project Structure

This project now follows the standard Maven directory layout:

```
process-logs/
├── pom.xml                    # Maven build configuration
├── src/
│   ├── main/
│   │   ├── java/             # Application source code
│   │   │   └── com/
│   │   │       └── s4apps/
│   │   │           └── processlog/   # Package directory
│   │   └── resources/        # Application resources
│   └── test/
│       ├── java/             # Test source code
│       └── resources/        # Test resources
└── target/                    # Build output (generated)
```

## Building with Maven

Build the project and create the shaded jar:

```bash
mvn -q -DskipTests package
```

Run a main class (example):

```bash
./run.sh com.s4apps.processlog.ProcessLog -v
```


## Dependencies

- **Jakarta Persistence API 3.1.0** - JPA interfaces
- **Hibernate ORM 6.4.4.Final** - JPA implementation
- **Hibernate HikariCP 6.4.4.Final** - HikariCP integration
- **Apache Commons CLI 1.11.0** - Command line argument parsing
- **Apache Commons Lang3 3.20.0** - Utility functions
- **MySQL Connector/J 9.6.0** - MySQL database connectivity

## Database Secrets

Database credentials are loaded from `src/main/resources/secrets.properties` at runtime.
You can override these with environment variables `DB_URL`, `DB_USER`, and `DB_PASSWORD`.

Sample `secrets.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/processlog
db.user=processlog
db.password=change-me
```

# IgnoreReason flags
0 = No reason to ignore = good data

The other fields are added together to form the reason:
REASON_UNKNOWN = 1;
REASON_IP = 2;
REASON_URL = 4;
REASON_SERVER = 8;
REASON_METHOD = 16;