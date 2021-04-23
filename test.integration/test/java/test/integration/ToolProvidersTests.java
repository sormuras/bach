package test.integration;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.core.ToolProviders;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class ToolProvidersTests {

  @Test
  void describeBach() {
    var bach = ToolProvider.findFirst("bach").orElseThrow();
    assertLinesMatch(
        """
          bach \\(com\\.github\\.sormuras\\.bach.*\\)
            Build modular Java projects""".lines(),
        ToolProviders.describe(bach).lines());
  }
}
