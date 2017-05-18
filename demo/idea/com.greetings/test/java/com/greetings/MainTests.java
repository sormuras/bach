package com.greetings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MainTests {

  @Test
  void main() {
    Assertions.assertEquals("Main", Main.class.getSimpleName());
  }
}
