package com.greetings;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class MainTests {

  @Test
  void executePublicStaticVoidMain() {
    assertDoesNotThrow(() -> Main.main(new String[0]));
  }
}
