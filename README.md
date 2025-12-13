# Suprim OSS

Open-source type-safe SQL query builder for PostgreSQL, MySQL, and MariaDB.

## Modules

| Module | Description |
|--------|-------------|
| `suprim-core` | Dialects, types, annotations, and query builder |
| `suprim-processor` | Annotation processor for generating metamodels |
| `suprim-jdbc` | Query executor and transaction management |

## Installation

### Maven

```xml
<!-- Core only (query building) -->
<dependency>
    <groupId>dev.suprim</groupId>
    <artifactId>suprim-core</artifactId>
    <version>0.0.1</version>
</dependency>

<!-- Full stack (includes core) -->
<dependency>
    <groupId>dev.suprim</groupId>
    <artifactId>suprim-jdbc</artifactId>
    <version>0.0.1</version>
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
                        <version>0.0.1</version>
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

### JDBC Executor

```java
// Create executor
SuprimExecutor executor = SuprimExecutor.create(dataSource);

// Query with mapping
List<User> users = executor.query(query, EntityMapper.of(User.class));

// Transactions
executor.transaction(tx -> {
    tx.execute(insertQuery);
    tx.execute(updateQuery);
});
```

## License

MIT
