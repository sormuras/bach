package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.UnsupportedActionException;
import org.junit.jupiter.api.Test;

class ActionTests {
  @Test
  void factoryThrowsOnUnsupportedAction() {
    assertThrows(UnsupportedActionException.class, () -> Action.ofCli(""));
  }
}
