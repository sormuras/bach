package foo;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

// @org.junit.platform.commons.annotation.Testable
public class Tester implements ToolProvider {

  public static void main(String... args) {
    new Tester().run(System.out, System.err, args);
  }

  @Override
  public String name() {
    return "test(" + getClass().getModule() + ")";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    dump(out, this);
    dump(out, new PublicFoo());
    dump(out, new PrivateFoo());
    return 0;
  }

  private static void dump(PrintWriter out, Object object) {
    out.print("\n" + object.getClass().getModule());
    out.print(" - " + object.getClass().getPackage());
    out.println(" - " + object.getClass());
    out.println(" > " + object);
  }
}
