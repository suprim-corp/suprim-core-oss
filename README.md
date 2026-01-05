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
    <version>0.0.8-SNAPSHOT</version>
</dependency>

<!-- Full stack (includes core) -->
<dependency>
    <groupId>dev.suprim</groupId>
    <artifactId>suprim-jdbc</artifactId>
    <version>0.0.8-SNAPSHOT</version>
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
                        <version>0.0.8-SNAPSHOT</version>
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

### EXISTS Subqueries

Combine predicates with EXISTS/NOT EXISTS for complex conditional logic:

```java
// Predicate-level: combine column condition with EXISTS using OR
SelectBuilder modelsSubquery = Suprim.selectRaw("1")
    .from(AttachedModel_.TABLE)
    .whereRaw("model_id = apps.id");

Suprim.select(App_.ID, App_.NAME)
    .from(App_.TABLE)
    .where(App_.WORKSPACE_ID.eq(workspaceId))
    .and(App_.TYPE.ne("SIMPLE").orExists(modelsSubquery))
    .build(dialect);
// SQL: WHERE workspace_id = ? AND (type != 'SIMPLE' OR EXISTS (SELECT 1 FROM ...))

// Static factory: Suprim.exists() for flexible composition
.where(App_.TYPE.ne("SIMPLE").or(Suprim.exists(modelsSubquery)))

// Builder-level: top-level OR EXISTS
.where(User_.IS_ACTIVE.eq(true))
.orExists(ordersSubquery)
// SQL: WHERE is_active = true OR EXISTS (...)
```

| Method | Description |
|--------|-------------|
| `predicate.orExists(subquery)` | Combine with OR EXISTS |
| `predicate.orNotExists(subquery)` | Combine with OR NOT EXISTS |
| `predicate.andExists(subquery)` | Combine with AND EXISTS |
| `predicate.andNotExists(subquery)` | Combine with AND NOT EXISTS |
| `Suprim.exists(subquery)` | Create EXISTS predicate |
| `Suprim.notExists(subquery)` | Create NOT EXISTS predicate |
| `.orExists(subquery)` | Builder-level OR EXISTS |
| `.orNotExists(subquery)` | Builder-level OR NOT EXISTS |

### Closure-Based Grouping

Laravel-style grouped conditions with automatic parentheses:

```java
// Group conditions with closures - auto-wrapped in parentheses
Suprim.select(User_.ID)
    .from(User_.TABLE)
    .where(User_.IS_ACTIVE.eq(true))
    .and(q -> q.where(User_.ROLE.eq("ADMIN")).or(User_.ROLE.eq("MOD")))
    .build(dialect);
// SQL: WHERE is_active = true AND (role = 'ADMIN' OR role = 'MOD')

// Combine closures with EXISTS
.and(q -> q.where(App_.TYPE.ne("SIMPLE")).orExists(modelsSubquery))
// SQL: AND (type != 'SIMPLE' OR EXISTS (...))

// OR grouping
.where(User_.TYPE.eq("PREMIUM"))
.or(q -> q.where(User_.CREDITS.gt(100)).and(User_.VERIFIED.eq(true)))
// SQL: WHERE type = 'PREMIUM' OR (credits > 100 AND verified = true)
```

| Method | Description |
|--------|-------------|
| `.where(q -> ...)` | Set WHERE with grouped conditions |
| `.and(q -> ...)` | Add AND with grouped conditions |
| `.or(q -> ...)` | Add OR with grouped conditions |

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

// BETWEEN query examples
executor.find(User.class)
    .whereBetween("age", 18, 65)           // Find users with age between 18 and 65
    .whereNotBetween("salary", 0, 1000)    // Exclude users with salary between 0-1000
    .get();
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

### Active Record Pattern

Extend `SuprimEntity` for full CRUD methods on your entities:

```java
@Entity(table = "users")
public class User extends SuprimEntity {
    @Id(strategy = GenerationType.UUID_V7)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email")
    private String email;

    // getters/setters
}
```

#### Auto-commit Mode

Register the executor once at startup, then call CRUD methods anywhere:

```java
// At app startup
SuprimContext.setGlobalExecutor(executor);

// Anywhere in your code - no transaction wrapper needed
User user = new User();
user.setEmail("test@example.com");
user.save();  // Auto-commits immediately

user.setEmail("updated@example.com");
user.update();  // Auto-commits

user.refresh();  // Reload from DB

user.delete();  // Auto-commits
```

#### Transaction Mode

For atomic multi-entity operations:

```java
executor.transaction(tx -> {
    User user = new User();
    user.setEmail("test@example.com");
    user.save();

    Profile profile = new Profile();
    profile.setUserId(user.getId());
    profile.save();

    // Both commit together or rollback on error
});
```

| Method      | Description                          |
|-------------|--------------------------------------|
| `save()`    | INSERT new entity, auto-generates ID |
| `update()`  | UPDATE existing entity by ID         |
| `delete()`  | DELETE entity by ID                  |
| `refresh()` | Reload entity values from DB         |

### Soft Delete

Soft delete marks records as deleted without removing them from the database.

#### Setup

Add `@SoftDeletes` annotation and a timestamp column:

```java
@Entity(table = "users")
@SoftDeletes  // Uses "deleted_at" column by default
public class User extends SuprimEntity {
    @Id(strategy = GenerationType.UUID_V7)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email")
    private String email;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // Or Instant, OffsetDateTime, Timestamp

    // getters/setters
}
```

Custom column name:

```java
@SoftDeletes(column = "removed_at")
```

#### Active Record Methods

```java
// Soft delete - sets deleted_at to NOW()
user.softDelete();

// Check if soft-deleted
if (user.isTrashed()) {
    // Restore - sets deleted_at to NULL
    user.restore();
}

// Permanent delete - removes from database
user.forceDelete();
```

#### Query Scopes

Soft-deleted records are **automatically excluded** from queries:

```java
// Default: only non-deleted records (WHERE deleted_at IS NULL)
Suprim.select(User_.ID, User_.EMAIL)
    .from(User_.TABLE)
    .build();

// Include soft-deleted records (no filter)
Suprim.select(User_.ID, User_.EMAIL)
    .from(User_.TABLE)
    .withTrashed()
    .build();

// Only soft-deleted records (WHERE deleted_at IS NOT NULL)
Suprim.select(User_.ID, User_.EMAIL)
    .from(User_.TABLE)
    .onlyTrashed()
    .build();
```

#### Bulk Operations

```java
// Soft delete multiple records
Suprim.softDelete(User_.TABLE, "deleted_at")
    .where(User_.IS_ACTIVE.eq(false))
    .build();

// Restore multiple records
Suprim.restore(User_.TABLE, "deleted_at")
    .whereRaw("deleted_at > NOW() - INTERVAL '30 days'")
    .build();
```

| Method          | Description                         |
|-----------------|-------------------------------------|
| `softDelete()`  | Set deleted_at to current timestamp |
| `restore()`     | Set deleted_at to NULL              |
| `forceDelete()` | Permanently delete from database    |
| `isTrashed()`   | Check if record is soft-deleted     |
| `withTrashed()` | Include soft-deleted in query       |
| `onlyTrashed()` | Query only soft-deleted records     |

## License

Suprim is open-sourced software licensed under the [MIT license](https://opensource.org/licenses/MIT).
