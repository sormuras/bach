package de.sormuras.bach.demo;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DemoMainTests {
  @Test
  void mainMethodExists() throws ReflectiveOperationException {
    assertNotNull(DemoMain.class.getMethod("main", String[].class));
  }
}
