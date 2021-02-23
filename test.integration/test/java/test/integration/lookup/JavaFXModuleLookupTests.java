package test.integration.lookup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.lookup.ModuleLookup;
import org.junit.jupiter.api.Test;

class JavaFXModuleLookupTests {

  @Test
  void checkJavaFX() {
    var fx = ModuleLookup.ofJavaFX("99");

    assertFalse(fx.lookupUri("foo").isPresent());

    assertTrue(fx.lookupUri("javafx.base").isPresent());
    assertTrue(fx.lookupUri("javafx.controls").isPresent());
    assertTrue(fx.lookupUri("javafx.fxml").isPresent());
    assertTrue(fx.lookupUri("javafx.graphics").isPresent());
    assertTrue(fx.lookupUri("javafx.media").isPresent());
    assertTrue(fx.lookupUri("javafx.swing").isPresent());
    assertTrue(fx.lookupUri("javafx.web").isPresent());
  }
}
