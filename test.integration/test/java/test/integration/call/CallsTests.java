package test.integration.call;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.tool.JDepsCall;
import com.github.sormuras.bach.tool.JLinkCall;
import com.github.sormuras.bach.tool.JPackageCall;
import com.github.sormuras.bach.tool.JarCall;
import com.github.sormuras.bach.tool.JavaCall;
import com.github.sormuras.bach.tool.JavacCall;
import com.github.sormuras.bach.tool.JavadocCall;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CallsTests {

  @ParameterizedTest
  @ValueSource(
      classes = {
        JarCall.class,
        JavaCall.class,
        JavacCall.class,
        JavadocCall.class,
        JDepsCall.class,
        JLinkCall.class,
        JPackageCall.class
      })
  void createViaDefaultConstructor(Class<? extends ToolCall<?>> type) throws Exception {
    var tool = type.getConstructor().newInstance();
    assertEquals(type.getSimpleName().toLowerCase(Locale.ROOT), tool.name() + "call");
    assertTrue(tool.arguments().isEmpty()); // empty by default
    assertSame(tool, tool.arguments(tool.arguments())); // reset same arguments is a noop
  }
}
