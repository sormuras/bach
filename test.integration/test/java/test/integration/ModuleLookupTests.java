package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.ModuleLookup;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModuleLookupTests {

  @Test
  void ofModuleUri() {
    var ab = ModuleLookup.of("m", "u");
    assertEquals("m -> u", ab.toString());
    assertEquals("u", ab.lookupModule("m").orElseThrow());
    assertFalse(ab.lookupModule("u").isPresent());
  }

  @Test
  void ofMap() {
    assertSame(ModuleLookup.ofEmpty(), ModuleLookup.of(Map.of()));
    assertEquals(ModuleLookup.of("m", "u"), ModuleLookup.of(Map.of("m", "u")));
  }
}
