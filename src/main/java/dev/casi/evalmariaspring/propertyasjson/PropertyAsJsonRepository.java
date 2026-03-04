package dev.casi.evalmariaspring.propertyasjson;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyAsJsonRepository extends JpaRepository<PropertyAsJson, Long> {

  @Query(
      value =
          "SELECT * FROM properties_as_json WHERE MATCH(title, description, city) AGAINST (:query IN BOOLEAN MODE)",
      countQuery =
          "SELECT COUNT(*) FROM properties_as_json WHERE MATCH(title, description, city) AGAINST (:query IN BOOLEAN MODE)",
      nativeQuery = true)
  Page<PropertyAsJson> fullTextSearch(@Param("query") String query, Pageable pageable);

  /**
   * Search JSON content using MariaDB JSON_SEARCH across specific paths. This finds rows where
   * JSON_SEARCH returns a non-NULL path for the given search string. Note: JSON_SEARCH performs
   * exact matches of the searched string. For partial matches consider using JSON_EXTRACT(... )
   * LIKE CONCAT('%', :query, '%') instead.
   */
  @Query(
      value =
          "SELECT * FROM properties_as_json WHERE "
              + "JSON_SEARCH(data, 'one', :query, NULL, '$.title') IS NOT NULL OR "
              + "JSON_SEARCH(data, 'one', :query, NULL, '$.description') IS NOT NULL OR "
              + "JSON_SEARCH(data, 'one', :query, NULL, '$.city') IS NOT NULL",
      countQuery =
          "SELECT COUNT(*) FROM properties_as_json WHERE "
              + "JSON_SEARCH(data, 'one', :query, NULL, '$.title') IS NOT NULL OR "
              + "JSON_SEARCH(data, 'one', :query, NULL, '$.description') IS NOT NULL OR "
              + "JSON_SEARCH(data, 'one', :query, NULL, '$.city') IS NOT NULL",
      nativeQuery = true)
  Page<PropertyAsJson> fullTextSearchJson(@Param("query") String query, Pageable pageable);

  // ── Filter by city (generated column) ──────────────────────────────

  @Query(
      value = "SELECT * FROM properties_as_json WHERE city = :city",
      countQuery = "SELECT COUNT(*) FROM properties_as_json WHERE city = :city",
      nativeQuery = true)
  Page<PropertyAsJson> filterByCity(@Param("city") String city, Pageable pageable);

  // ── Fulltext search sorted by title ────────────────────────────────

  @Query(
      value =
          "SELECT * FROM properties_as_json WHERE MATCH(title, description, city) AGAINST (:query IN BOOLEAN MODE) ORDER BY title",
      countQuery =
          "SELECT COUNT(*) FROM properties_as_json WHERE MATCH(title, description, city) AGAINST (:query IN BOOLEAN MODE)",
      nativeQuery = true)
  Page<PropertyAsJson> fullTextSearchSorted(@Param("query") String query, Pageable pageable);

  // ── Combined: fulltext + city filter + sort by title ───────────────

  @Query(
      value =
          "SELECT * FROM properties_as_json WHERE MATCH(title, description, city) AGAINST (:query IN BOOLEAN MODE) AND city = :city ORDER BY title",
      countQuery =
          "SELECT COUNT(*) FROM properties_as_json WHERE MATCH(title, description, city) AGAINST (:query IN BOOLEAN MODE) AND city = :city",
      nativeQuery = true)
  Page<PropertyAsJson> fullTextSearchFilteredAndSorted(
      @Param("query") String query, @Param("city") String city, Pageable pageable);
}
