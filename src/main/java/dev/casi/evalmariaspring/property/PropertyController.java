package dev.casi.evalmariaspring.property;

import dev.casi.evalmariaspring.PropertyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class PropertyController {

  private final PropertyService propertyService;

  @GetMapping("/api/properties")
  public ResponseEntity<Page<Property>> listProperties(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.getProperties(pageable));
  }

  @GetMapping("/api/properties/search")
  public ResponseEntity<Page<PropertyDto>> searchProperties(
      @RequestParam String q, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.searchProperties(q, pageable));
  }

  @GetMapping("/api/properties/filter")
  public ResponseEntity<Page<PropertyDto>> filterByCity(
      @RequestParam String city, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.filterByCity(city, pageable));
  }

  @GetMapping("/api/properties/search-sorted")
  public ResponseEntity<Page<PropertyDto>> searchPropertiesSorted(
      @RequestParam String q, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.searchPropertiesSorted(q, pageable));
  }

  @GetMapping("/api/properties/search-filtered-sorted")
  public ResponseEntity<Page<PropertyDto>> searchPropertiesFilteredAndSorted(
      @RequestParam String q,
      @RequestParam(required = false) String city,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.searchPropertiesFilteredAndSorted(q, city, pageable));
  }
}
