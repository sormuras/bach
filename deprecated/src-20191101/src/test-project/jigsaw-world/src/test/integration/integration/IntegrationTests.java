package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

class IntegrationTests {

  @Test
  void accessWorld() {
    assertEquals("world", org.astro.World.name());
  }

  @Test
  void accessGreetings() throws ClassNotFoundException {
    // assertEquals("Main", com.greetings.Main.class.getSimpleName()); // Does not even compile!

    var main = Class.forName("com.greetings.Main"); // Loading a hidden class is "okay"...

    var exception =
        assertThrows(
            IllegalAccessException.class,
            () -> main.getMethod("main", String[].class).invoke(null, (Object) null));

    assertEquals(
        "class integration.IntegrationTests (in module integration)"
            + " cannot access class com.greetings.Main (in module com.greetings)"
            + " because module com.greetings"
            + " does not export com.greetings"
            + " to module integration",
        exception.getMessage());
  }

  @Test
  @DisabledOnJre(JRE.JAVA_8)
  void disabledOnJava8() {}
}
