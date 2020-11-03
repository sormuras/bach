package test.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void versionIsNotNull() {
    assertNotNull(Bach.version());
  }

  @Test
  void versionIsParsable() {
    var version = Bach.version();
    assertEquals(version, ModuleDescriptor.Version.parse(version).toString());
  }
}
