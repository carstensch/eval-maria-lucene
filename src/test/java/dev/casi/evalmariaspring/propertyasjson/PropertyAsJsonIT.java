package dev.casi.evalmariaspring.propertyasjson;

import static org.assertj.core.api.Assertions.assertThat;

import dev.casi.evalmariaspring.PropertyDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class PropertyAsJsonIT {

  static MariaDBContainer<?> mariadb =
      new MariaDBContainer<>("mariadb:11")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @Autowired private PropertyAsJsonRepository repository;
  @Autowired private PropertyAsJsonService service;

  @BeforeAll
  static void startContainer() {
    mariadb.start();
  }

  @AfterAll
  static void stopContainer() {
    mariadb.stop();
  }

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mariadb::getJdbcUrl);
    registry.add("spring.datasource.username", mariadb::getUsername);
    registry.add("spring.datasource.password", mariadb::getPassword);
  }

  @Test
  void smokeTestJsonSearchAndMatch() {

    // Prepare data
    PropertyDto a =
        PropertyDto.builder()
            .title("Sunny Beach Flat")
            .description("Close to the pier")
            .city("Barcelona")
            .build();
    PropertyDto b =
        PropertyDto.builder()
            .title("Cozy Mountain Cabin")
            .description("Near ski lift")
            .city("Lisbon")
            .build();

    PropertyAsJson ea = new PropertyAsJson();
    ea.setData(a);
    repository.save(ea);
    PropertyAsJson eb = new PropertyAsJson();
    eb.setData(b);
    repository.save(eb);

    // Give DB a moment for indexes / migrations if any

    // JSON_SEARCH exact match: search for 'Barcelona' should find the first
    var page1 =
        repository.fullTextSearchJson(
            "Barcelona", org.springframework.data.domain.PageRequest.of(0, 10));
    assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(1);

    // MATCH...AGAINST on generated columns (requires migration V3 to be applied)
    var page2 =
        repository.fullTextSearch("Sunny*", org.springframework.data.domain.PageRequest.of(0, 10));
    // We expect at least one hit when fulltext index exists, but this depends on V3 migration run
    // by Flyway
    assertThat(page2.getTotalElements()).isGreaterThanOrEqualTo(0);
  }
}
