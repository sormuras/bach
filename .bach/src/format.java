import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;

class format {
  public static void main(String... args) throws Exception {
    System.out.printf("Formatting Java source files...%n");

    var call = ToolCall.of("format@1.15.0").with("--replace").withFindFiles("**.java");
    var bach = Bach.ofDefaults();
    bach.run("install", "format@1.15.0");
    bach.run(call);

    System.out.printf("%5d .java files formatted%n", call.arguments().size() - 1);
  }
}
