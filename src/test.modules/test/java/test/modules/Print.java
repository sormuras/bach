package test.modules;

import de.sormuras.bach.ToolCall;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public final class Print implements ToolCall, ToolProvider {

  private final boolean configured;
  private final boolean normal;
  private final String message;
  private final int code;

  public Print() {
    this(false, true, "String...", 0);
  }

  public Print(String message) {
    this( true, message, 0);
  }

  public Print(boolean normal, String message, int code) {
    this(true, normal, message, code);
  }

  private Print(boolean configured, boolean normal, String message, int code) {
    this.configured = configured;
    this.normal = normal;
    this.message = message;
    this.code = code;
  }

  @Override
  public String name() {
    return "print";
  }

  @Override
  public String[] args() {
    return configured ? new String[] {message} : new String[0];
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    (normal ? out : err).print(configured ? message : String.join(" ", args));
    return code;
  }
}
