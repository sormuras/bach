package test.integration.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.tool.JDeps;
import com.github.sormuras.bach.tool.JLink;
import com.github.sormuras.bach.tool.JPackage;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Java;
import com.github.sormuras.bach.tool.Javac;
import com.github.sormuras.bach.tool.Javadoc;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ToolsTests {

  @ParameterizedTest
  @ValueSource(
      classes = {
        Jar.class,
        Java.class,
        Javac.class,
        Javadoc.class,
        JDeps.class,
        JLink.class,
        JPackage.class
      })
  void createViaDefaultConstructor(Class<? extends Command<?>> type) throws Exception {
    var tool = type.getConstructor().newInstance();
    assertEquals(type.getSimpleName().toLowerCase(Locale.ROOT), tool.name());
    assertTrue(tool.arguments().isEmpty()); // empty by default
    assertSame(tool, tool.arguments(tool.arguments())); // reset same arguments is a noop
  }
}
