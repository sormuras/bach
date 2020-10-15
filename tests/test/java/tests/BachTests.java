package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void modular() {
    assertEquals("com.github.sormuras.bach", Bach.class.getModule().getName());
    assertNotNull(new Bach().toString());
  }

  @Test
  void loadBachByItsToolName() {
    assertTrue(ToolProvider.findFirst("bach").isPresent());
  }
}
