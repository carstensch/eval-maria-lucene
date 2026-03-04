# Concept & Recommendation: Migration from Lucene to MariaDB Native Full-Text Search

## 1. Summary

This document describes a migration strategy for the gradual transition from
Hibernate Search / Lucene to native MariaDB full-text search with JSON documents and
generated columns. It evaluates the advantages and disadvantages of both approaches and provides a clear
recommendation based on the PoC results.

---

## 2. Current State

| Aspect               | Current                                              |
|----------------------|------------------------------------------------------|
| Search Engine        | Hibernate Search 7.x with Lucene Backend             |
| Database             | MariaDB 11.7                                         |
| Index Synchronization| Automatic Listeners + MassIndexer on Startup         |
| Data Volume          | 100,000 Property Records (scalable to 1 Mio.)        |

### Identified Problems with Current State

- **Operational Complexity**: Lucene index must be managed as a filesystem directory (backup, consistency in
  cluster deployments).
- **Dual-State**: Data exists in MariaDB *and* in the Lucene index — synchronization errors are possible.
- **Scaling**: Each application instance requires its own local index or a shared solution (
  Elasticsearch/OpenSearch), creating infrastructure overhead.

---

## 3. Target State

A single MariaDB instance handles both data persistence and full-text search:

- **JSON Column** (`data JSON`) stores property data as a document.
- **Generated Columns** (`title`, `description`, `city` as `STORED GENERATED`) extract the relevant fields.
- **FULLTEXT INDEX** on generated columns enables `MATCH ... AGAINST` in Boolean Mode.

---

## 4. Migration Strategy (Phases)

### Phase 1 – Dual Write (2–4 weeks)

1. Roll out new schema `properties_as_json` in parallel with existing `properties` schema (Flyway V2, V3).
2. Mirror write operations (INSERT/UPDATE/DELETE) to **both** tables.
3. Read operations continue to use Lucene.
4. **Validation**: Nightly job compares Lucene results with MariaDB full-text results for a defined
   query set.

### Phase 2 – Shadow Read (1–2 weeks)

1. Production traffic is executed in parallel against both engines (Feature Flag).
2. Results are compared; deviations are logged.
3. Latency metrics for both paths are captured with Micrometer / Prometheus.

### Phase 3 – Cutover (1 week)

1. Feature Flag switches primary read path to MariaDB.
2. Lucene remains active as fallback (read-only).
3. Monitoring of error rate and latency.

### Phase 4 – Cleanup (1–2 weeks)

1. Remove Lucene dependencies (`hibernate-search-*`).
2. Remove `BuildLuceneIndexOnStartupListener`, `@Indexed` annotations, and index directory.
3. Optional: Replace `properties` table with `properties_as_json` or retain both.

---

## 5. Comparison: Lucene vs. MariaDB Native Full-Text Search

### 5.1 Lucene (Hibernate Search)

| ✅ Advantages                                 | ❌ Disadvantages                                        |
|-----------------------------------------------|--------------------------------------------------------|
| Very fast full-text search (In-Memory Index)  | Separate index on filesystem (backup, consistency)     |
| Relevance Scoring (BM25, TF-IDF)              | MassIndexer runtime for large data volumes             |
| Fuzzy Search, Phonetics, Synonyms             | No trivial cluster deployment (Lucene = local)         |
| Powerful Query DSL (Bool, Phrase, Wildcard)   | Additional dependency (hibernate-search, lucene-core)  |
| Sorting by arbitrary fields                   | Dual-State: Data + Index must remain synchronized      |

### 5.2 MariaDB FULLTEXT on Generated Columns

| ✅ Advantages                                     | ❌ Disadvantages                                        |
|----------------------------------------------------|--------------------------------------------------------|
| **Single Source of Truth** — no separate index    | Boolean-Mode Wildcard less powerful than Lucene DSL   |
| No synchronization issues                        | No true relevance scoring in BOOLEAN MODE              |
| Simple backup (DB dump is sufficient)            | No Fuzzy / Phonetics / Synonym support                 |
| JSON + generated columns = flexible schema       | FULLTEXT index updates slower on bulk inserts          |
| No additional deployment (no Elasticsearch)      | No facets / aggregations                               |
| Horizontal scaling via MariaDB Galera Cluster    | Performance on very complex queries potentially worse  |

### 5.3 MariaDB JSON_SEARCH (Direct JSON Query)

| ✅ Advantages                                | ❌ Disadvantages                                      |
|---------------------------------------------|------------------------------------------------------|
| No additional index needed                  | **Significantly slower** — Full Table Scan             |
| Flexible: search arbitrary JSON paths       | Exact matches only (no prefix, no fuzzy)              |
|                                             | Not suitable for production with large data volumes   |

---

## 6. PoC Results

The benchmark results can be retrieved live via the UI:

1. `docker compose up -d && ./mvnw spring-boot:run`
2. Open browser: [http://localhost:8080](http://localhost:8080)
3. Click "Run Benchmark"

### Expected Result Trends (100K Rows)

| Scenario                 | Lucene         | MariaDB MATCH..AGAINST | MariaDB JSON_SEARCH |
|--------------------------|----------------|------------------------|---------------------|
| Full-Text Search         | ⚡ Very Fast   | ✅ Fast                | ❌ Slow              |
| Filter by City           | ⚡ Very Fast   | ✅ Fast                | –                   |
| Full-Text + Sort         | ⚡ Very Fast   | ✅ Fast                | –                   |
| Full-Text + Filter + Sort| ⚡ Very Fast   | ✅ Fast                | –                   |

> **Note**: The exact millisecond values depend on hardware.
> Lucene is typically faster for pure full-text search, but MariaDB MATCH..AGAINST
> is sufficiently performant for the identified use cases.

---

## 7. Recommendation

### For the identified use cases (Full-Text, Filter, Sorting):

**→ MariaDB FULLTEXT on generated columns (JSON approach) is recommended.**

#### Rationale:

1. **Operational Simplification**: One fewer component to operate, monitor, and secure.
2. **Consistency**: No dual-state problem — the database is the single source of truth.
3. **Sufficient Performance**: For full-text search with filter and sorting over 100K–1M rows, MariaDB
   FULLTEXT with generated columns delivers acceptable response times.
4. **Flexibility**: JSON documents allow schema changes without DDL migration (simply add new fields to JSON).
5. **Cluster-Ready**: MariaDB Galera Cluster enables horizontal scaling without additional infrastructure.

### When Lucene / Elasticsearch should be retained:

- **Fuzzy Search** (typo tolerance) is business-critical.
- **Relevance Scoring** (BM25) is needed for result ranking.
- **Faceted Search** (aggregations, e.g., "10 houses in Barcelona, 5 in Madrid") is desired.
- **Synonym Expansion** or **language-specific Analyzers** (stemming, stopwords) are required.
- Data volume grows significantly beyond 10 million rows.

### Conclusion

For the current requirements profile (full-text + filter + sorting on real estate data up to
approximately 1 million records), MariaDB native full-text search offers better cost-benefit ratio.
The phased migration minimizes risk and allows rollback at any time.

