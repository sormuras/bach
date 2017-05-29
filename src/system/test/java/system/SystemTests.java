package system;

import de.sormuras.solartools.Main;
import java.io.Serializable;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SystemTests {
  @Test
  void classMainImplementsSerializable() {
    Assertions.assertTrue(List.of(Main.class.getInterfaces()).contains(Serializable.class));
  }
}
