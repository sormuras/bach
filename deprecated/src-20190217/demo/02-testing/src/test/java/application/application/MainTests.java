package application;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MainTests {

  @Test
  void main() throws NoSuchMethodException {
    int modifiers = Main.class.getDeclaredMethod("main", String[].class).getModifiers();
    Assertions.assertTrue(Modifier.isStatic(modifiers));
  }
}
