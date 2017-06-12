import java.io.PrintWriter;
import java.nio.file.Paths;
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
    assert Bach.Layout.BASIC == Bach.Layout.of(Paths.get("demo/basic"));
    assert Bach.Layout.FIRST == Bach.Layout.of(Paths.get("demo/idea"));
    assert Bach.Layout.TRAIL == Bach.Layout.of(Paths.get("demo/common"));
  }

  private static void provideTool() {
    Bach bach = new Bach();
    bach.set(new ExampleTool());
    bach.call("example", "123");
  }

  public static void main(String[] args) {
    buildLayout();
    provideTool();
    CommandTests.main(args);
    FolderTests.main(args);
  }
}
