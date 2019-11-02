// default package

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MakeCommandTests {

  @Test
  void constructor() {
    var command = new Make.Command("name", List.of());
    assertEquals("name", command.name);
    assertEquals(0, command.arguments.size());
  }
}
