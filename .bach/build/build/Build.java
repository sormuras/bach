package build;

import com.github.sormuras.bach.Bach;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class Build implements ToolProvider {

  public static void main(String... args) {
    new Build().run(System.out, System.err, args);
  }

  @Override
  public String name() {
    return "build";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("Build " + Bach.class.getModule());
    return 0;
  }
}
