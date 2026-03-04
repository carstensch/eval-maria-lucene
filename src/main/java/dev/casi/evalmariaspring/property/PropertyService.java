package dev.casi.evalmariaspring.property;

import dev.casi.evalmariaspring.PropertyDto;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final EntityManager entityManager;
  private final PropertyConverter propertyConverter;

  // ── Traditional (relational) ──────────────────────────────────────

  public Page<Property> getProperties(Pageable pageable) {
    return propertyRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Page<PropertyDto> searchProperties(String query, Pageable pageable) {
    SearchSession searchSession = Search.session(entityManager);
    long offset = pageable.getOffset();
    int limit = pageable.getPageSize();
    var result =
        searchSession
            .search(Property.class)
            .where(f -> f.match().fields("title", "description", "city").matching(query))
            .fetch((int) offset, limit);

    final List<PropertyDto> data = result.hits().stream().map(propertyConverter::convert).toList();

    return new PageImpl<>(data, pageable, result.total().hitCount());
  }

  // ── Filter by city ─────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public Page<PropertyDto> filterByCity(String city, Pageable pageable) {
    SearchSession searchSession = Search.session(entityManager);
    long offset = pageable.getOffset();
    int limit = pageable.getPageSize();
    var result =
        searchSession
            .search(Property.class)
            .where(f -> f.match().field("filter_city").matching(city))
            .fetch((int) offset, limit);

    final List<PropertyDto> data = result.hits().stream().map(propertyConverter::convert).toList();
    return new PageImpl<>(data, pageable, result.total().hitCount());
  }

  // ── Sort by title ──────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public Page<PropertyDto> searchPropertiesSorted(String query, Pageable pageable) {
    SearchSession searchSession = Search.session(entityManager);
    long offset = pageable.getOffset();
    int limit = pageable.getPageSize();
    var result =
        searchSession
            .search(Property.class)
            .where(f -> f.match().fields("title", "description", "city").matching(query))
            .sort(SearchSortFactory::score)
            .sort(f -> f.field("sort_title"))
            .fetch((int) offset, limit);

    final List<PropertyDto> data = result.hits().stream().map(propertyConverter::convert).toList();
    return new PageImpl<>(data, pageable, result.total().hitCount());
  }

  // ── Combined: fulltext + city filter + sort by title ───────────────

  @Transactional(readOnly = true)
  public Page<PropertyDto> searchPropertiesFilteredAndSorted(
      String query, String city, Pageable pageable) {
    SearchSession searchSession = Search.session(entityManager);
    long offset = pageable.getOffset();
    int limit = pageable.getPageSize();
    var result =
        searchSession
            .search(Property.class)
            .where(
                f -> {
                  BooleanPredicateClausesStep<?> bool =
                      f.bool()
                          .must(f.match().fields("title", "description", "city").matching(query));
                  if (city != null && !city.isBlank()) {
                    bool.filter(f.match().field("filter_city").matching(city));
                  }
                  return bool;
                })
            .sort(f -> f.field("sort_title"))
            .fetch((int) offset, limit);

    final List<PropertyDto> data = result.hits().stream().map(propertyConverter::convert).toList();
    return new PageImpl<>(data, pageable, result.total().hitCount());
  }
}
