package com.github.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MainTests {
  @Test
  void noop() {
    assertNotNull(new Main().toString());
  }
}
