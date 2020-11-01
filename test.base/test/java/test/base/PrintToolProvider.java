package test.base;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record PrintToolProvider(boolean configured, boolean normal, String message, int code)
    implements ToolProvider {

  public PrintToolProvider() {
    this(false, true, "String...", 0);
  }

  public PrintToolProvider(String message) {
    this(true, message, 0);
  }

  public PrintToolProvider(boolean normal, String message, int code) {
    this(true, normal, message, code);
  }

  @Override
  public String name() {
    return "print";
  }

  // @Override
  public String[] args() {
    return configured ? new String[] {message} : new String[0];
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    (normal ? out : err).print(configured ? message : String.join(" ", args));
    return code;
  }
}
