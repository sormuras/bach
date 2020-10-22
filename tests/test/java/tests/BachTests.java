package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void classBachResidesInNamedModule() {
    assertEquals("com.github.sormuras.bach", Bach.class.getModule().getName());
  }

  @Test
  void versionOfBachIsNotNullAndNotBlack() {
    var version = Bach.version();
    assertNotNull(version);
    assertFalse(version.isBlank());
  }
}
