package dev.casi.evalmariaspring.propertyasjson;

import dev.casi.evalmariaspring.PropertyDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PropertyAsJsonConverter implements Converter<PropertyAsJson, PropertyDto> {

  @Override
  public PropertyDto convert(PropertyAsJson source) {

    final PropertyDto dto = source.getData();
    dto.setId(source.getId());

    return dto;
  }
}
