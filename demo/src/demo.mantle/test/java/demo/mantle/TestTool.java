package demo.mantle;

public class TestTool implements java.util.spi.ToolProvider {
  @Override
  public String name() {
    return "test(demo.mantle)";
  }

  @Override
  public int run(java.io.PrintWriter out, java.io.PrintWriter err, String... args) {
    try {
      checkMantle();
      return 0;
    } catch (Throwable throwable) {
      throwable.printStackTrace(err);
      return 1;
    }
  }

  private static void checkMantle() {
    var mantle = new Mantle();
    var type = mantle.core.getClass();
    if (!type.getSimpleName().equals("PublicCore")) {
      throw new AssertionError("Expected PublicCore, but got: " + type);
    }
  }
}
