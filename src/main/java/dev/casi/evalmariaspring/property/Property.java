package dev.casi.evalmariaspring.property;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Table(name = "properties")
@Getter
@Setter
@Indexed
public class Property {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  @FullTextField
  @KeywordField(name = "sort_title", sortable = Sortable.YES)
  private String title;

  @Column(nullable = false)
  @FullTextField
  private String description;

  @Column(nullable = false)
  @FullTextField
  @KeywordField(name = "filter_city", sortable = Sortable.YES)
  private String city;
}
