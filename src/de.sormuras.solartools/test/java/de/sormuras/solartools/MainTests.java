package de.sormuras.solartools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MainTests {

  @Test
  void main() throws Exception {
    Assertions.assertFalse(Main.class.isInterface());
    Assertions.assertEquals(1, Main.class.getMethod("main", String[].class).getParameterCount());
  }
}
