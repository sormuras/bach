package integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IntegrationTests {

  @Test
  void usage() {
    Assertions.assertNotNull(new application.Main());
    Assertions.assertNotNull(application.api.Plugin.load());
    Assertions.assertNotNull(application.api.Version.VERSION);
  }
}
