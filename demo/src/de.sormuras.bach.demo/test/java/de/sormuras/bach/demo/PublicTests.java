package de.sormuras.bach.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class PublicTests {
  @Test
  void instantiate() {
    assertDoesNotThrow(PublicClass::new);
  }
}
