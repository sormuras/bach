package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class ToolProvidersTests {

  @Test
  void describeBach() {
    var bach = ToolProvider.findFirst("bach").orElseThrow();
    assertLinesMatch(
        """
          bach                 \\(EXTERNAL, com\\.github\\.sormuras\\.bach.*\\)
              Builds (on(ly)) Java Modules
              https://github.com/sormuras/bach"""
            .lines(),
        ToolProviders.describe(bach).lines());
  }
}
