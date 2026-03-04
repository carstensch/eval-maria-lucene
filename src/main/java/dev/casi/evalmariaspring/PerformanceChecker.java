package dev.casi.evalmariaspring;

import dev.casi.evalmariaspring.property.PropertyService;
import dev.casi.evalmariaspring.propertyasjson.PropertyAsJsonService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PerformanceChecker {
  private static final int WARMUP_RUNS = 2;
  private static final int MEASURED_RUNS = 5;
  private final PropertyAsJsonService propertyAsJsonService;
  private final PropertyService propertyService;

  @PostMapping("/api/performance/check")
  public ResponseEntity<List<BenchmarkResult>> performance() {

    final List<String> textQueries =
        List.of(
            "Charming",
            "Madrid",
            "House",
            "Elegant apartment",
            "villa",
            "Valencia",
            "Modern",
            "Flat",
            "Seville");
    final List<String> cities = List.of("Madrid", "Barcelona", "Lisbon", "Valencia", "Seville");
    final Pageable pageable = Pageable.ofSize(100);

    final List<BenchmarkResult> results = new ArrayList<>();

    // ── Scenario 1: Fulltext search ──────────────────────────────────

    results.add(
        benchmark(
            "Fulltext",
            "Lucene (Hibernate Search)",
            () -> {
              long hits = 0;
              for (String q : textQueries) {
                hits += propertyService.searchProperties(q, pageable).getTotalElements();
              }
              return hits;
            }));

    results.add(
        benchmark(
            "Fulltext",
            "MariaDB MATCH..AGAINST",
            () -> {
              long hits = 0;
              for (String q : textQueries) {
                hits +=
                    propertyAsJsonService.searchPropertiesAsJson(q, pageable).getTotalElements();
              }
              return hits;
            }));

    results.add(
        benchmark(
            "Fulltext",
            "MariaDB JSON_SEARCH",
            () -> {
              long hits = 0;
              for (String q : textQueries) {
                hits += propertyAsJsonService.fullTextSearchJson(q, pageable).getTotalElements();
              }
              return hits;
            }));

    // ── Scenario 2: Filter by city ───────────────────────────────────

    results.add(
        benchmark(
            "Filter by city",
            "Lucene (Hibernate Search)",
            () -> {
              long hits = 0;
              for (String city : cities) {
                hits += propertyService.filterByCity(city, pageable).getTotalElements();
              }
              return hits;
            }));

    results.add(
        benchmark(
            "Filter by city",
            "MariaDB generated column",
            () -> {
              long hits = 0;
              for (String city : cities) {
                hits += propertyAsJsonService.filterByCity(city, pageable).getTotalElements();
              }
              return hits;
            }));

    // ── Scenario 3: Fulltext + sort by title ─────────────────────────

    results.add(
        benchmark(
            "Fulltext + sort",
            "Lucene (Hibernate Search)",
            () -> {
              long hits = 0;
              for (String q : textQueries) {
                hits += propertyService.searchPropertiesSorted(q, pageable).getTotalElements();
              }
              return hits;
            }));

    results.add(
        benchmark(
            "Fulltext + sort",
            "MariaDB MATCH..AGAINST + ORDER BY",
            () -> {
              long hits = 0;
              for (String q : textQueries) {
                hits +=
                    propertyAsJsonService
                        .searchPropertiesAsJsonSorted(q, pageable)
                        .getTotalElements();
              }
              return hits;
            }));

    // ── Scenario 4: Fulltext + city filter + sort ────────────────────

    results.add(
        benchmark(
            "Fulltext + filter + sort",
            "Lucene (Hibernate Search)",
            () -> {
              long hits = 0;
              for (int i = 0; i < textQueries.size(); i++) {
                String city = cities.get(i % cities.size());
                hits +=
                    propertyService
                        .searchPropertiesFilteredAndSorted(textQueries.get(i), city, pageable)
                        .getTotalElements();
              }
              return hits;
            }));

    results.add(
        benchmark(
            "Fulltext + filter + sort",
            "MariaDB MATCH..AGAINST + WHERE + ORDER BY",
            () -> {
              long hits = 0;
              for (int i = 0; i < textQueries.size(); i++) {
                String city = cities.get(i % cities.size());
                hits +=
                    propertyAsJsonService
                        .searchPropertiesAsJsonFilteredAndSorted(textQueries.get(i), city, pageable)
                        .getTotalElements();
              }
              return hits;
            }));

    return ResponseEntity.ok(results);
  }

  private BenchmarkResult benchmark(String scenario, String engine, Supplier<Long> work) {
    // Warm-up
    for (int i = 0; i < WARMUP_RUNS; i++) {
      work.get();
    }
    // Measured runs
    long totalMs = 0;
    long totalHits = 0;
    for (int i = 0; i < MEASURED_RUNS; i++) {
      long start = System.nanoTime();
      totalHits = work.get();
      long elapsed = (System.nanoTime() - start) / 1_000_000;
      totalMs += elapsed;
    }
    long avgMs = totalMs / MEASURED_RUNS;
    return new BenchmarkResult(scenario, engine, avgMs, totalHits, MEASURED_RUNS);
  }

  public record BenchmarkResult(
      String scenario, String engine, long durationMs, long totalHits, int runs) {}
}
