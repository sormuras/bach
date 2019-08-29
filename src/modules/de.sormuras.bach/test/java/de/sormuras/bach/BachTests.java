package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BachTests (de.sormuras.bach)")
class BachTests {
  @Test
  void instantiate() {
    assertNotNull(new Bach().toString());
  }

  @Test
  void banner() {
    assertFalse(new Bach().getBanner().isBlank());
  }

  @Test
  void version() {
    assertDoesNotThrow(() -> ModuleDescriptor.Version.parse(Bach.VERSION));
  }
}
