package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PropertyTests {

  @ParameterizedTest
  @EnumSource(Property.class)
  void values(Property property) {
    assertEquals(property.getDefaultValue(), property.get());
  }
}
