package test.modules;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void versionIsNotNull() {
    assertNotNull(Bach.version());
  }
}
