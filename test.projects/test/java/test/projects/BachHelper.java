package test.projects;

import com.github.sormuras.bach.Printer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

class BachHelper {

  static Printer errorPrinter() {
    var out = new PrintWriter(Writer.nullWriter());
    var err = new PrintWriter(System.err, true);
    return new Printer(out, err);
  }

  static String run(String name, Object... arguments) {
    var args = Stream.of(arguments).map(Object::toString).toArray(String[]::new);
    var out = new StringWriter();
    var writer = new PrintWriter(out);
    ToolProvider.findFirst(name).orElseThrow().run(writer, writer, args);
    return out.toString();
  }
}
