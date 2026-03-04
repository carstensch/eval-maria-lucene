package dev.casi.evalmariaspring.propertyasjson;

import dev.casi.evalmariaspring.PropertyDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyAsJsonService {

  private final PropertyAsJsonRepository propertyAsJsonRepository;
  private final PropertyAsJsonConverter converter;

  public Page<PropertyDto> getPropertiesAsJson(Pageable pageable) {
    return propertyAsJsonRepository.findAll(pageable).map(converter::convert);
  }

  /**
   * Perform a fulltext search using MariaDB MATCH...AGAINST in BOOLEAN MODE. This method converts a
   * user query into a boolean-mode prefix search by appending '*' to each token, allowing
   * wildcard/prefix matching.
   *
   * <p>Example: "sunny barc" -> "sunny* barc*"
   */
  public Page<PropertyDto> searchPropertiesAsJson(String query, Pageable pageable) {
    String booleanQuery = toBooleanWildcardQuery(query);
    if (booleanQuery.isEmpty()) {
      return Page.empty(pageable);
    }
    return propertyAsJsonRepository.fullTextSearch(booleanQuery, pageable).map(converter::convert);
  }

  public Page<PropertyDto> fullTextSearchJson(String query, Pageable pageable) {
    return propertyAsJsonRepository.fullTextSearchJson(query, pageable).map(converter::convert);
  }

  // ── Filter by city ─────────────────────────────────────────────────

  public Page<PropertyDto> filterByCity(String city, Pageable pageable) {
    return propertyAsJsonRepository.filterByCity(city, pageable).map(converter::convert);
  }

  // ── Fulltext search sorted by title ────────────────────────────────

  public Page<PropertyDto> searchPropertiesAsJsonSorted(String query, Pageable pageable) {
    String booleanQuery = toBooleanWildcardQuery(query);
    if (booleanQuery.isEmpty()) {
      return Page.empty(pageable);
    }
    return propertyAsJsonRepository
        .fullTextSearchSorted(booleanQuery, pageable)
        .map(converter::convert);
  }

  // ── Combined: fulltext + city filter + sort by title ───────────────

  public Page<PropertyDto> searchPropertiesAsJsonFilteredAndSorted(
      String query, String city, Pageable pageable) {
    String booleanQuery = toBooleanWildcardQuery(query);
    if (booleanQuery.isEmpty()) {
      return Page.empty(pageable);
    }
    if (city == null || city.isBlank()) {
      return propertyAsJsonRepository
          .fullTextSearchSorted(booleanQuery, pageable)
          .map(converter::convert);
    }
    return propertyAsJsonRepository
        .fullTextSearchFilteredAndSorted(booleanQuery, city, pageable)
        .map(converter::convert);
  }

  // Convert user input into a boolean-mode wildcard query
  private String toBooleanWildcardQuery(String q) {
    if (q == null) {
      return "";
    }
    String trimmed = q.trim();
    if (trimmed.isEmpty()) {
      return "";
    }

    String[] tokens = trimmed.split("\\s+");
    List<String> parts = new ArrayList<>();

    for (String raw : tokens) {
      // Keep letters/numbers and some diacritics; remove other punctuation.
      // \p{L} = any kind of letter from any language; \p{N} = any kind of numeric
      String cleaned = raw.replaceAll("[^\\p{L}\\p{N}]+", "");
      if (cleaned.isEmpty()) {
        continue;
      }

      // If user already supplied a wildcard or boolean operator, preserve it.
      if (cleaned.endsWith("*") || cleaned.startsWith("+") || cleaned.startsWith("-")) {
        parts.add(cleaned);
      } else {
        // Make it a prefix wildcard so MATCH...AGAINST can match prefixes
        parts.add(cleaned + "*");
      }
    }

    return String.join(" ", parts);
  }
}
