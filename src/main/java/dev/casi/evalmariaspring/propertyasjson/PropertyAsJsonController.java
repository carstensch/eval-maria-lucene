package dev.casi.evalmariaspring.propertyasjson;

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
public class PropertyAsJsonController {

  private final PropertyAsJsonService propertyService;

  @GetMapping("/api/properties-json")
  public ResponseEntity<Page<PropertyDto>> list(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.getPropertiesAsJson(pageable));
  }

  @GetMapping("/api/properties-json/search")
  public ResponseEntity<Page<PropertyDto>> search(
      @RequestParam("q") String q, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.searchPropertiesAsJson(q, pageable));
  }

  @GetMapping("/api/properties-json/json-search")
  public ResponseEntity<Page<PropertyDto>> searchJson(
      @RequestParam("q") String q, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.fullTextSearchJson(q, pageable));
  }

  @GetMapping("/api/properties-json/filter")
  public ResponseEntity<Page<PropertyDto>> filterByCity(
      @RequestParam String city, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.filterByCity(city, pageable));
  }

  @GetMapping("/api/properties-json/search-sorted")
  public ResponseEntity<Page<PropertyDto>> searchSorted(
      @RequestParam("q") String q, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(propertyService.searchPropertiesAsJsonSorted(q, pageable));
  }

  @GetMapping("/api/properties-json/search-filtered-sorted")
  public ResponseEntity<Page<PropertyDto>> searchFilteredSorted(
      @RequestParam("q") String q,
      @RequestParam(required = false) String city,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        propertyService.searchPropertiesAsJsonFilteredAndSorted(q, city, pageable));
  }
}
