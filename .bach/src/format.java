import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;

class format {
  public static void main(String... args) throws Exception {
    var bach = Bach.ofDefaults();
    if (bach.configuration().finder().find("format@1.15.0").isEmpty()) {
      bach.run(
          """
          load-and-verify
            .bach/external-tools/format@1.15.0/google-java-format-1.15.0-all-deps.jar
            https://github.com/google/google-java-format/releases/download/v1.15.0/google-java-format-1.15.0-all-deps.jar\
          #SIZE=3519780\
          &SHA-256=a356bb0236b29c57a3ab678f17a7b027aad603b0960c183a18f1fe322e4f38ea
          """);
    }
    var format = ToolCall.of("format@1.15.0").with("--replace").withFindFiles("**/*.java");
    bach.run("banner", "Format %d .java files".formatted(format.arguments().size() - 1));
    bach.run(format);
  }
}
