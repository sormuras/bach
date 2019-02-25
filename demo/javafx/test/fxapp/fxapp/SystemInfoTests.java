package fxapp;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class SystemInfoTests {

  @Test
  void checkJavaVersionIsPresent() {
    assertFalse(SystemInfo.javaVersion().isBlank());
  }
}
