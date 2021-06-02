package com.github.sormuras.bach;

import org.junit.jupiter.api.Test;
import test.base.SwallowSystem;

class MainTests {
  @Test
  @SwallowSystem
  void version() {
    Main.main("--version");
  }
}
