# Eval Maria Lucene – Full-Text Search Performance Comparison

A Spring Boot application demonstrating a **migration strategy from Lucene (Hibernate Search) to native MariaDB full-text search**. This project includes a performance benchmark UI that compares Lucene, MariaDB MATCH..AGAINST, and MariaDB JSON_SEARCH approaches on real estate property data.

## 🎯 Project Overview

This PoC (Proof of Concept) evaluates different full-text search strategies:

- **Lucene (Hibernate Search)**: Traditional in-memory index backend
- **MariaDB FULLTEXT**: Native database full-text search on generated columns
- **MariaDB JSON_SEARCH**: Direct JSON document querying

The application generates 100,000 property records and benchmarks various search scenarios to help determine the best migration path.

---

## 📋 Prerequisites

- **Java 21** or later
- **Docker & Docker Compose** (for MariaDB)
- **Maven** (or use the included `mvnw` wrapper)
- **Git** (optional, for cloning)

---

## 🚀 Quick Start

### 1. Start the MariaDB Database

```bash
docker compose up -d
```

This starts a MariaDB 11.7 container with the following credentials:
- **Database**: `mydatabase`
- **User**: `myuser`
- **Password**: `secret`
- **Root Password**: `verysecret`

### 2. Run the Spring Boot Application

Using Maven wrapper (recommended):
```bash
./mvnw spring-boot:run
```

Or with Maven directly:
```bash
mvn spring-boot:run
```

The application will:
- Create the database schema via Flyway migrations
- Generate 100,000 property records
- Initialize Lucene index
- Start the Spring Boot server on **http://localhost:8080**

### 3. Open the Web UI

Navigate to:
```
http://localhost:8080
```

You should see the Performance Comparison dashboard.

---

## 📊 Using the Benchmark UI

The web interface allows you to run performance benchmarks:

1. **Click "Run Benchmark"** button on the home page
2. The application will execute multiple search scenarios:
   - **Fulltext Search**: Searches across all text fields
   - **Filter by City**: Filtering without full-text matching
   - **Fulltext + Sort**: Full-text search with result sorting
   - **Fulltext + Filter + Sort**: Combined operations

3. **Results Display**:
   - Bar chart comparing execution times across three engines
   - Detailed results table with timings in milliseconds
   - Charts update in real-time as benchmarks complete

### Benchmark Scenarios

Each scenario measures:
- **Lucene (Hibernate Search)**: Via `PropertyService`
- **MariaDB MATCH..AGAINST**: Via `PropertyAsJsonService` using FULLTEXT index
- **MariaDB JSON_SEARCH**: Direct JSON document search (for comparison)

---

## 🏗️ Project Structure

```
eval-maria-lucene/
├── src/main/java/dev/casi/evalmariaspring/
│   ├── EvalMariaSpringApplication.java       # Spring Boot entry point
│   ├── PerformanceChecker.java               # Benchmark REST endpoint
│   ├── PropertyDataGenerator.java            # Generates 100K property records
│   ├── PropertyDto.java                      # Data transfer object
│   ├── property/                             # Lucene-based search (legacy)
│   │   ├── Property.java
│   │   ├── PropertyService.java
│   │   ├── PropertyController.java
│   │   └── PropertyRepository.java
│   └── propertyasjson/                       # MariaDB native search (new)
│       ├── PropertyAsJson.java
│       ├── PropertyAsJsonService.java
│       ├── PropertyAsJsonController.java
│       └── PropertyAsJsonRepository.java
├── src/main/resources/
│   ├── application.yaml                      # Spring configuration
│   ├── db/migration/
│   │   ├── V1__create_properties_table.sql   # Legacy schema
│   │   ├── V2__create_properties_as_json_table.sql  # New JSON schema
│   │   └── V3__add_fulltext_index_to_properties_as_json.sql
│   └── static/index.html                     # Benchmark UI
├── compose.yaml                              # Docker Compose for MariaDB
└── pom.xml                                   # Maven dependencies
```

---

## 🔧 Configuration

### Database Connection

Edit `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/mydatabase
    username: myuser
    password: secret
  jpa:
    hibernate:
      ddl-auto: validate
```

### Lucene Index Location

The Lucene index is stored at:
```
target/.lucene-index
```

Change this in `application.yaml` if needed:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        search:
          backend:
            directory:
              root: /path/to/index
```

---

## 📚 REST API Endpoints

### Property Search (Lucene)

```bash
GET /api/properties/search?query=Madrid&page=0&size=20
```

### Property Search (MariaDB Native)

```bash
GET /api/properties-json/search?query=Madrid&page=0&size=20
```

### Run Performance Benchmark

```bash
POST /api/performance/check
```

Returns a JSON array of benchmark results:
```json
[
  {
    "scenario": "Fulltext",
    "engine": "Lucene (Hibernate Search)",
    "duration_ms": 45
  },
  {
    "scenario": "Fulltext",
    "engine": "MariaDB MATCH..AGAINST",
    "duration_ms": 52
  }
]
```

---

## 🗄️ Database Schema

### Lucene-based Schema (Legacy)

```sql
CREATE TABLE properties (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  description LONGTEXT,
  city VARCHAR(255),
  price DECIMAL(10,2)
);
```

### MariaDB JSON Schema (New)

```sql
CREATE TABLE properties_as_json (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  data JSON NOT NULL,
  
  -- Generated columns for full-text indexing
  title VARCHAR(255) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(data, '$.title'))),
  description LONGTEXT GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(data, '$.description'))),
  city VARCHAR(255) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(data, '$.city'))),
  
  -- Full-text index
  FULLTEXT INDEX ft_properties (title, description, city)
);
```

---

## 🧪 Building and Testing

### Build the Project

```bash
./mvnw clean package
```

### Run Tests

```bash
./mvnw test
```

### Build Docker Image (Optional)

```bash
./mvnw spring-boot:build-image
```

---

## 📈 Performance Metrics

The benchmark measures:
- **Warm-up runs**: 2 (to allow JIT compilation)
- **Measured runs**: 5 (average of these is reported)
- **Data volume**: 100,000 property records
- **Search queries**: 9 different text patterns
- **Cities**: 5 different cities for filtering

### Expected Results (on standard hardware)

| Scenario | Lucene | MariaDB FULLTEXT | MariaDB JSON_SEARCH |
|----------|--------|------------------|---------------------|
| Full-Text Search | ~40ms | ~50ms | ~200ms |
| Filter by City | ~10ms | ~15ms | – |
| Full-Text + Sort | ~60ms | ~75ms | – |
| Full-Text + Filter + Sort | ~80ms | ~100ms | – |

*Note: Actual timings depend on hardware and data size.*

---

## 🔄 Migration Phases (From RECOMMENDATION.md)

See [RECOMMENDATION.md](RECOMMENDATION.md) for detailed migration strategy:

1. **Phase 1 – Dual Write** (2–4 weeks)
   - Write to both tables, read from Lucene
   - Validate consistency

2. **Phase 2 – Shadow Read** (1–2 weeks)
   - Execute reads against both engines
   - Compare results and latency

3. **Phase 3 – Cutover** (1 week)
   - Switch primary read path to MariaDB
   - Lucene as fallback

4. **Phase 4 – Cleanup** (1–2 weeks)
   - Remove Lucene dependencies
   - Clean up old code and indexes

---

## 🐛 Troubleshooting

### Database Connection Refused

```
ERROR: Connection refused
```

**Solution**: Ensure MariaDB container is running:
```bash
docker compose up -d
docker compose ps
```

### Lucene Index Corruption

```
ERROR: IOException reading index
```

**Solution**: Delete the index directory and restart:
```bash
rm -rf target/.lucene-index
./mvnw spring-boot:run
```

### Out of Memory During Benchmark

```
java.lang.OutOfMemoryError
```

**Solution**: Increase JVM heap:
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g"
```

### Port Already in Use

```
ERROR: Address already in use: bind
```

**Solution**: Change the port in `application.yaml`:
```yaml
server:
  port: 8081
```

---

## 📖 Documentation

- **[RECOMMENDATION.md](RECOMMENDATION.md)** – Detailed migration strategy and comparison
- **[Interviewtask_Technical_Lead_Java.pdf](Interviewtask_Technical_Lead_Java.pdf)** – Original requirements

---

## 📦 Dependencies

Key dependencies:
- **Spring Boot 3.5.11** – Framework
- **Spring Data JPA** – Database ORM
- **Hibernate Search 7.2.1** – Lucene integration
- **MariaDB JDBC Driver** – Database connectivity
- **Flyway** – Database migrations
- **Lombok** – Boilerplate reduction

Full dependency list: See [pom.xml](pom.xml)

---

## 🎓 Learning Resources

- [Hibernate Search Documentation](https://hibernate.org/search/)
- [MariaDB Full-Text Search](https://mariadb.com/docs/reference/feature/full-text-search/)
- [MariaDB Generated Columns](https://mariadb.com/docs/reference/sql-statements/constraint-syntax/generated-columns/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

---

## 📝 License

This project is part of a technical evaluation. See included PDF for context.

---

## ✨ Next Steps

After exploring the benchmark results:

1. Review [RECOMMENDATION.md](RECOMMENDATION.md) for the full migration strategy
2. Assess which search approach fits your use case
3. Plan implementation of Phase 1 (Dual Write)
4. Monitor performance metrics during migration
5. Execute phased cutover as detailed in the recommendation

---

## 🤝 Support

For questions or issues:
1. Check the [RECOMMENDATION.md](RECOMMENDATION.md) for strategy guidance
2. Review the benchmark results in the UI
3. Examine the code in `src/main/java` for implementation details

