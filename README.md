# Suprim OSS

[![codecov](https://codecov.io/gh/suprim-corp/suprim-core-oss/branch/main/graph/badge.svg)](https://codecov.io/gh/suprim-corp/suprim-core-oss)

Open-source type-safe SQL query builder for PostgreSQL, MySQL, and MariaDB.

## Modules

| Module             | Description                                     |
|--------------------|-------------------------------------------------|
| `suprim-core`      | Dialects, types, annotations, and query builder |
| `suprim-processor` | Annotation processor for generating metamodels  |
| `suprim-jdbc`      | Query executor and transaction management       |

## Installation

### Maven

```xml
<!-- Core only (query building) -->
<dependency>
    <groupId>dev.suprim</groupId>
    <artifactId>suprim-core</artifactId>
    <version>0.0.3</version>
</dependency>

<!-- Full stack (includes core) -->
<dependency>
    <groupId>dev.suprim</groupId>
    <artifactId>suprim-jdbc</artifactId>
    <version>0.0.3</version>
</dependency>
```

### Annotation Processor (for metamodel generation)

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>dev.suprim</groupId>
                        <artifactId>suprim-processor</artifactId>
                        <version>0.0.3</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Supported Databases

- PostgreSQL (all versions)
- MySQL 5.7+
- MySQL 8.0+
- MariaDB 10.3+

Oracle, SQL Server, and DB2 support coming soon.

## Usage

### Query Builder

```java
// Auto-detect dialect from JDBC URL
SqlDialect dialect = DialectRegistry.detectFromUrl(jdbcUrl);

// Build type-safe queries
QueryResult query = Suprim.select(User_.ID, User_.EMAIL)
    .from(User_.TABLE)
    .where(User_.EMAIL.eq("test@example.com"))
    .orderBy(User_.CREATED_AT.desc())
    .limit(10)
    .build(dialect);

String sql = query.sql();
List<Object> params = query.parameters();
```

### SQL Functions

```java

// Date/Time
Fn.now()                                    // NOW()
Fn.currentDate()                            // CURRENT_DATE
Fn.currentTimestamp()                       // CURRENT_TIMESTAMP

// String functions
Fn.upper(User_.NAME)                        // UPPER("name")
Fn.lower(User_.EMAIL)                       // LOWER("email")
Fn.concat(User_.FIRST, " ", User_.LAST)     // CONCAT("first", ' ', "last")
Fn.coalesce(User_.NICKNAME, "Anonymous")    // COALESCE("nickname", 'Anonymous')

// Math functions
Fn.abs(Order_.AMOUNT)                       // ABS("amount")
Fn.round(Order_.TOTAL, 2)                   // ROUND("total", 2)

// Custom functions
Fn.call("my_function", arg1, arg2)          // my_function(arg1, arg2)

// Raw expressions
Fn.raw("EXTRACT(YEAR FROM ?)", User_.CREATED_AT)

// Usage in queries
Suprim.select(User_.ID, Fn.now().as("current_time"))
    .from(User_.TABLE)
    .where(User_.CREATED_AT.lt(Fn.now()))
    .build(dialect);

Suprim.insertInto(User_.TABLE)
    .column(User_.CREATED_AT, Fn.now())
    .build(dialect);
```

### ID Generation

```java
// UUID v7 (time-ordered, recommended)
@Id(strategy = GenerationType.UUID_V7)
private UUID id;

// UUID v4 (random)
@Id(strategy = GenerationType.UUID_V4)
private UUID id;

// Database auto-increment
@Id(strategy = GenerationType.IDENTITY)
private Long id;

// Database-generated UUID
@Id(strategy = GenerationType.UUID_DB)
private UUID id;

// Custom generator
@Id(generator = MyIdGenerator.class)
private String id;
```

### JDBC Executor

```java
// Create executor
SuprimExecutor executor = SuprimExecutor.create(dataSource);

// Query with mapping
List<User> users = executor.query(query, EntityMapper.of(User.class));

// Transactions with entity persistence
executor.transaction(tx -> {
    User user = new User();
    user.setEmail("test@example.com");

    User saved = tx.save(user);  // ID generated automatically
    System.out.println(saved.getId());

    tx.execute(updateQuery);
});
```

## License

Suprim is open-sourced software licensed under the [MIT license](https://opensource.org/licenses/MIT).
