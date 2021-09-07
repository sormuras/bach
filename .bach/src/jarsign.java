import com.github.sormuras.bach.ToolCall;

class jarsign {
  public static void main(String... args) {
    try (var bach = new com.github.sormuras.bach.Bach(args)) {
      var jarsigner = bach.path().javaExecutable().resolveSibling("jarsigner");
      bach.run(ToolCall.process(jarsigner, "--help")).visit(bach.printer()::print);
    }
  }
}
