package application.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PluginTests {

  @Test
  void load() {
    int expected = getClass().getModule().isNamed() ? 1 : 0;
    Assertions.assertEquals(expected, Plugin.load().size());
  }
}
