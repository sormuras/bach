package org.astro;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorldTests {

  @Test
  @DisplayName("Answer to the Ultimate Question of Life, the Universe, and Everything ... is 42")
  void answerIs42() {
    Assertions.assertEquals(42, World.answer);
  }

  @Test
  void nameIsWorld() {
    Assertions.assertEquals("world", World.name());
  }
}
