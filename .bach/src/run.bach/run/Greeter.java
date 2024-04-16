package run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import run.bach.Bach;
import run.bach.Tool;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;

public record Greeter(String version) implements ToolInstaller {
  public static void main(String... args) {
    var greeter = new Greeter("99");
    greeter.install().run(); // "run.bach/greeter"

    var folders = Bach.Folders.ofCurrentWorkingDirectory();
    Tool.of("greeter/eager", greeter.installInto(folders.tools("greeter/eager"))).run();
    Tool.of("greeter/inert", () -> greeter.installInto(folders.tools("greeter/inert"))).run();
  }

  @Override
  public ToolProvider installInto(Path folder) {
    var file = folder.resolve("Prog.java");
    if (!Files.exists(file)) {
      try {
        Files.createDirectories(folder);
        var text = // language=java
            """
            class Prog {
              public static void main(String... args) {
                System.out.println("Greetings!");
              }
            }
            """;
        Files.writeString(file, text);
      } catch (Exception exception) {
        throw new RuntimeException();
      }
    }
    return ToolProgram.java(file.toString());
  }
}
