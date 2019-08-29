package it;

import org.junit.jupiter.api.Test;
import java.util.spi.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTests {

  @Test
  void findBachUsingToolProviderAPI() {
    assertTrue(ToolProvider.findFirst("bach").isPresent(), "Tool 'bach' not found!");
  }
}
