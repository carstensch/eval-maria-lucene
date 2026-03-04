package dev.casi.evalmariaspring.property;

import dev.casi.evalmariaspring.PropertyDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class PropertyConverter implements Converter<Property, PropertyDto> {

  @Override
  public PropertyDto convert(Property source) {
    return PropertyDto.builder()
        .id(source.getId())
        .title(source.getTitle())
        .description(source.getDescription())
        .city(source.getCity())
        .build();
  }
}
