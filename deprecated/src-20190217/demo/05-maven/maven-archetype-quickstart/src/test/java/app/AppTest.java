package app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AppTest {

  @Test
  void simpleClassNameIsApp() {
    Assertions.assertEquals("App", App.class.getSimpleName());
  }
}
