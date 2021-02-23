package test.integration.lookup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.lookup.ModuleLookup;
import org.junit.jupiter.api.Test;

class LWJGLModuleLookupTests {

  @Test
  void checkLWJGL() {
    var lwjgl = ModuleLookup.ofLWJGL("99");

    assertFalse(lwjgl.lookupUri("foo").isPresent());

    assertTrue(lwjgl.lookupUri("org.lwjgl").isPresent());
    assertTrue(lwjgl.lookupUri("org.lwjgl.natives").isPresent());
    assertTrue(lwjgl.lookupUri("org.lwjgl.openal").isPresent());
    assertTrue(lwjgl.lookupUri("org.lwjgl.openal.natives").isPresent());
    assertTrue(lwjgl.lookupUri("org.lwjgl.opengl").isPresent());
    assertTrue(lwjgl.lookupUri("org.lwjgl.opengl.natives").isPresent());
    assertTrue(lwjgl.lookupUri("org.lwjgl.vulkan").isPresent());
    assertTrue(lwjgl.lookupUri("org.lwjgl.vulkan.natives").isPresent());
  }
}
