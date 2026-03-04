package dev.casi.evalmariaspring;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PropertyDto {

  private Long id;

  private String title;

  private String description;

  private String city;
}
