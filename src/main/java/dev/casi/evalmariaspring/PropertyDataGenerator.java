package dev.casi.evalmariaspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.casi.evalmariaspring.property.PropertyRepository;
import dev.casi.evalmariaspring.propertyasjson.PropertyAsJsonRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates up to 1,000,000 Property rows on application start if the table doesn't already contain
 * them.
 *
 * <p>Implementation notes: - Uses JdbcTemplate.batchUpdate to bypass JPA/Hibernate and avoid
 * triggering automatic indexing per-row. - Inserts in batches to avoid OOM and keep transactions
 * reasonably sized. - Skips generation when the table already contains the target number of rows. -
 * Also populates the {@code properties_as_json} table with the same data stored as JSON documents.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class PropertyDataGenerator implements ApplicationRunner {

  // Target and tuning parameters
  private static final long TARGET_COUNT = 100_000L;
  private static final int BATCH_SIZE = 1_000; // adjust if necessary
  private final JdbcTemplate jdbcTemplate;
  private final PropertyRepository propertyRepository;
  private final PropertyAsJsonRepository propertyAsJsonRepository;
  private final ObjectMapper objectMapper;
  private final Random random = new Random();

  @Override
  public void run(ApplicationArguments args) throws Exception {
    long existing = propertyRepository.count();
    if (existing >= TARGET_COUNT) {
      log.info(
          "Property generation skipped: table already contains {} rows (target {}).",
          existing,
          TARGET_COUNT);
      return;
    }

    long toInsert = TARGET_COUNT - existing;
    log.info(
        "Starting property generation: existing={}, target={}, toInsert={}",
        existing,
        TARGET_COUNT,
        toInsert);

    String sql = "INSERT INTO properties (title, description, city) VALUES (?, ?, ?);";

    long startTime = System.currentTimeMillis();
    long inserted = 0L;
    List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);

    for (long i = 0; i < toInsert; i++) {
      final String title = generateTitle(existing + i + 1);

      batchArgs.add(new Object[] {title, title + generateDescription(), randomCity()});

      if (batchArgs.size() >= BATCH_SIZE) {
        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        inserted += results.length;
        log.info("Inserted batch: {} rows (total inserted so far={})", results.length, inserted);
        batchArgs.clear();
      }
    }

    if (!batchArgs.isEmpty()) {
      int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
      inserted += results.length;
      log.info("Inserted final batch: {} rows (total inserted={})", results.length, inserted);
      batchArgs.clear();
    }

    long durationMs = System.currentTimeMillis() - startTime;
    log.info("Completed property generation. Inserted {} rows in {} ms", inserted, durationMs);

    // Also populate the JSON table
    generateJsonProperties();
  }

  private void generateJsonProperties() throws Exception {
    long existingJson = propertyAsJsonRepository.count();
    long existingProperties = propertyRepository.count();

    if (existingJson >= existingProperties) {
      log.info(
          "JSON property generation skipped: json table already contains {} rows (properties table has {}).",
          existingJson,
          existingProperties);
      return;
    }

    log.info(
        "Starting JSON property generation from existing properties: jsonRows={}, propertyRows={}",
        existingJson,
        existingProperties);

    String sql = "INSERT INTO properties_as_json (data) VALUES (?)";

    long startTime = System.currentTimeMillis();
    long inserted = 0L;
    int page = (int) (existingJson / BATCH_SIZE);

    while (true) {
      var pageRequest = org.springframework.data.domain.PageRequest.of(page, BATCH_SIZE);
      var propertyPage = propertyRepository.findAll(pageRequest);

      if (propertyPage.isEmpty()) {
        break;
      }

      List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);
      for (var property : propertyPage.getContent()) {
        PropertyDto dto =
            PropertyDto.builder()
                .title(property.getTitle())
                .description(property.getDescription())
                .city(property.getCity())
                .build();
        String json = objectMapper.writeValueAsString(dto);
        batchArgs.add(new Object[] {json});
      }

      int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
      inserted += results.length;
      log.info("Inserted JSON batch: {} rows (total inserted so far={})", results.length, inserted);

      if (!propertyPage.hasNext()) {
        break;
      }
      page++;
    }

    long durationMs = System.currentTimeMillis() - startTime;
    log.info(
        "Completed JSON property generation. Copied {} property rows as JSON in {} ms",
        inserted,
        durationMs);
  }

  private String generateTitle(long index) {
    // Short deterministic-ish title using index to keep uniqueness
    return "Property #" + index + " " + sampleAdjective() + " " + sampleType();
  }

  private String generateDescription() {
    // Randomized but short description
    return " with "
        + (2 + random.nextInt(4))
        + " rooms and "
        + (1 + random.nextInt(3))
        + " bathrooms.";
  }

  private String randomCity() {
    String[] cities = {
      "Lisbon",
      "Porto",
      "Madrid",
      "Barcelona",
      "Valencia",
      "Seville",
      "Bilbao",
      "Coimbra",
      "Braga",
      "Vigo"
    };
    return cities[random.nextInt(cities.length)];
  }

  private String sampleAdjective() {
    String[] words = {
      "Sunny",
      "Cozy",
      "Spacious",
      "Modern",
      "Charming",
      "Elegant",
      "Rustic",
      "Stylish",
      "Bright",
      "Luxurious"
    };
    return words[random.nextInt(words.length)];
  }

  private String sampleType() {
    String[] types = {
      "Studio", "Apartment", "Flat", "House", "Villa", "Loft", "Penthouse", "Townhouse"
    };
    return types[random.nextInt(types.length)];
  }
}
