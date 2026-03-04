package dev.casi.evalmariaspring.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class PropertyServiceTests {

  @Test
  void getPropertiesReportMapsEntityFieldsToReportShape() {
    Property property = new Property();
    property.setId(7L);
    property.setTitle("Beach Flat");
    property.setDescription("2-bedroom apartment close to the sea");
    property.setCity("Barcelona");

    Pageable pageable = PageRequest.of(0, 20);
    Page<Property> pageResult = new PageImpl<>(List.of(property), pageable, 1);

    PropertyRepository repository = Mockito.mock(PropertyRepository.class);
    when(repository.findAll(pageable)).thenReturn(pageResult);

    // Create service with mocked dependencies. Other constructor args aren't needed for
    // getProperties().
    PropertyService localService = new PropertyService(repository, null, null);
    Page<Property> reports = localService.getProperties(pageable);

    assertThat(reports.getContent()).hasSize(1);
    assertThat(reports.getContent().get(0).getId()).isEqualTo(7L);
    assertThat(reports.getContent().get(0).getTitle()).isEqualTo("Beach Flat");
    assertThat(reports.getContent().get(0).getDescription())
        .isEqualTo("2-bedroom apartment close to the sea");
    assertThat(reports.getContent().get(0).getCity()).isEqualTo("Barcelona");
  }
}
