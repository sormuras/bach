package run.demo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;
import run.bach.workflow.Folders;

public record Greeter(String version) implements ToolInstaller {
  public static void main(String... args) throws Exception {
    var greeter = new Greeter("99");
    System.out.println("[1]");
    greeter.install().run(); // "run.bach/greeter@99" in ".bach/tmp/tool"

    var folders = Folders.ofTemporaryDirectory();
    System.out.println("\n[2]");
    Tool.of("greeter/eager", greeter.install(folders.tool("greeter/eager"))).run();
    System.out.println("\n[3]");
    Tool.of("greeter/inert", () -> greeter.install(folders.tool("greeter/inert"))).run();
  }

  @Override
  public ToolProvider install(Path into) throws Exception {
    System.out.println(into.toUri());
    var file = into.resolve("Prog.java");
    if (!Files.exists(file)) {
      Files.createDirectories(into);
      var text = // language=java
          """
          class Prog {
            public static void main(String... args) {
              System.out.println("Greetings!");
            }
          }
          """;
      Files.writeString(file, text);
    }
    return ToolProgram.java(file.toString());
  }
}
