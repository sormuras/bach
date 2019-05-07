package scaffold.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScaffoldPluginTests {
  @Test
  void livingInModuleScaffold() {
    assertEquals("scaffold", ScaffoldPluginTests.class.getModule().getName());
  }
}
