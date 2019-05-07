package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.List;
import org.junit.jupiter.api.Test;
import scaffold.api.ScaffoldPlugin;

class IntegrationTests {
  @Test
  void echoPluginIsLoadedAndEchoesTheInput() {
    var actualLines = ScaffoldPlugin.forEach("one");
    assertLinesMatch(List.of("one"), actualLines);
  }

  @Test
  void livingInModuleIntegration() {
    assertEquals("integration", IntegrationTests.class.getModule().getName());
  }
}
