import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@BachExtension
class ActionTests {

  @ParameterizedTest
  @EnumSource(Bach.Action.class)
  void run(Bach.Action action, BachExtension.BachSupplier supplier, @TempDir Path temp) {
    var bach = supplier.get();
    bach.base = temp;
    var code = bach.run(List.of(action));
    assertEquals(0, code, supplier.toString());
    assertLinesMatch(
        List.of("DEBUG Running 1 task(s)...", ">> ACTION OUTPUT >>", "DEBUG << " + action),
        supplier.outLines());
    var errors = supplier.errLines();
    if (action == Bach.Action.TOOL) {
      assertLinesMatch(List.of("WARNING " + action + " not handled, yet"), errors);
    } else {
      assertTrue(errors.isEmpty(), errors.toString());
    }
  }
}
