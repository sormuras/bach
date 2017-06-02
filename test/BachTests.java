import java.io.PrintWriter;
import java.util.Arrays;
import java.util.spi.ToolProvider;

class BachTests {

  static class ExampleTool implements ToolProvider {

    @Override
    public String name() {
      return "example";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      out.println("Example with " + Arrays.toString(args));
      return 0;
    }
  }

  private static void buildLayout() {
    // assert Layout.BASIC == Bach.Builder.buildLayout(Paths.get("demo/basic"));
    // assert Layout.FIRST == Bach.Builder.buildLayout(Paths.get("demo/idea"));
    // assert Layout.TRAIL == Bach.Builder.buildLayout(Paths.get("demo/common"));
  }

  private static void provideTool() {
    Bach bach = new Bach();
    bach.set(new ExampleTool());
    bach.execute("example", "123");
  }

  public static void main(String[] args) {
    buildLayout();
    provideTool();
    CommandTests.main(args);
    FolderTests.main(args);
  }
}
