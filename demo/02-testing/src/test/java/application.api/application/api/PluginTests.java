package application.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PluginTests {

  @Test
  void load() {
    Assertions.assertEquals(0, Plugin.load().size());
  }
}
