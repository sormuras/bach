package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExternalModuleLocatorTests {
  @Test
  void broken() {
    var broken = new BrokenLocator();
    assertSame(ExternalModuleLocator.Stability.UNKNOWN, broken.stability());
    assertThrows(Exception.class, () -> broken.locate("m"));
  }

  private static class BrokenLocator implements ExternalModuleLocator {

    @Override
    public Optional<ExternalModuleLocation> locate(String module) {
      throw new RuntimeException("broken");
    }
  }
}
