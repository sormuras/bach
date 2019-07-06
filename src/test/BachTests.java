import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class BachTests {

  @Test
  @SwallowSystem
  void main() {
    assertDoesNotThrow((Executable) Bach::main);
  }

  @Test
  @SwallowSystem
  void mainWithIllegalArgument() {
    var e = assertThrows(Error.class, () -> Bach.main("illegal argument"));
    assertEquals("Bach.main(\"illegal argument\") failed with error code: 42", e.getMessage());
  }

  @Test
  void defaultValuesAreAssigned() {
    var bach = Bach.of();
    assertNotNull(bach.out);
    assertNotNull(bach.err);
    assertNotNull(bach.configuration);
  }

  @Test
  void mainWithEmptyListOfTools() {
    assertEquals(0, new Probe().bach.main(List.of()));
  }

  @Test
  void mainWithListOfCustomTools() {
    assertEquals(0, new Probe().bach.main(List.of("noop", "noop")));
  }
}
