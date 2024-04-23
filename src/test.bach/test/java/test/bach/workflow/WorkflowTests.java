package test.bach.workflow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import run.bach.workflow.Structure.Launcher;

class WorkflowTests {
  @Nested
  class StructureTests {
    @Nested
    class LauncherTests {
      @ParameterizedTest
      @ValueSource(strings = {"", "a", "=", "a=", "=a", "a=/", "a=a/", "a=/a"})
      void illegalArgumentThrowsIllegalArgumentException(String s) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Launcher.of(s));
      }
    }
  }
}
