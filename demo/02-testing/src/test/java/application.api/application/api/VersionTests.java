package application.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VersionTests {

  @Test
  void constants() {
    Assertions.assertEquals("9", Version.MAJOR); // MAJOR package-private is accessable
    // Assertions.assertEquals("123", Version.MINOR); error: MINOR has private access
    Assertions.assertEquals("9.123", Version.VERSION); // VERSION is declared public
  }
}
