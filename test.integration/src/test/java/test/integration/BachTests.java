package test.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void version() {
    assertNotNull(Bach.class.getModule().getDescriptor().toNameAndVersion());
  }
}
