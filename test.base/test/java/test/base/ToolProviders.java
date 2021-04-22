package test.base;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public class ToolProviders {

  public static String run(String name, Object... arguments) {
    var args = Stream.of(arguments).map(Object::toString).toArray(String[]::new);
    var out = new StringWriter();
    var writer = new PrintWriter(out);
    ToolProvider.findFirst(name).orElseThrow().run(writer, writer, args);
    return out.toString();
  }

  /** Hidden default constructor. */
  private ToolProviders() {}
}
