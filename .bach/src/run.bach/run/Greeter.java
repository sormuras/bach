package run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import run.bach.Bach;
import run.bach.Tool;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;

public record Greeter() implements ToolInstaller {
  public static void main(String... args) {
    var greeter = new Greeter();
    greeter.install().run(); // "external/greeter"

    var folders = Bach.Folders.ofCurrentWorkingDirectory();
    Tool.of("my/c@99", ToolProvider.findFirst("javac").orElseThrow()).run();
    Tool.of("greeter/eager", greeter.installInto(folders.tools("greeter/eager"))).run();
    Tool.of("greeter/inert", () -> greeter.installInto(folders.tools("greeter/inert"))).run();
  }

  @Override
  public String version() {
    return null;
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
