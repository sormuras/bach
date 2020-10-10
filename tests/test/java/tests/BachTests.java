package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void modular() {
    assertEquals("com.github.sormuras.bach", Bach.class.getModule().getName());
    assertNotNull(new Bach().toString());
  }
}
