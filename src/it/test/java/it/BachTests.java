package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Bach;
import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

public class BachTests {
  @Test
  public void moduleDescriptorParsesVersion() {
    assertDoesNotThrow(() -> ModuleDescriptor.Version.parse(Bach.VERSION));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse(""));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse("-"));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse("master"));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse("ea"));
  }
}
