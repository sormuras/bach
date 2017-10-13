package integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IntegrationTests {

  @Test
  void deepReflectionWorks() throws ReflectiveOperationException {
    Assertions.assertNotNull(Class.forName("application.Main").getConstructor().newInstance());
  }

  @Test
  void useExportedTypes() throws Exception {
    Assertions.assertNotNull(application.api.Plugin.load());
    Assertions.assertNotNull(application.api.Version.VERSION);
  }
}
