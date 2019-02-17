package application.api.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReverseTests {

  @Test
  void reverse() {
    Assertions.assertEquals("321", new Reverse().apply("123"));
  }
}
