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
    greeter.tool("0").run(); // "external/greeter@0"

    var folders = Bach.Folders.ofCurrentWorkingDirectory();
    Tool.of("greeter/eager@1", greeter.install("1", folders.tools("greeter/eager@1"))).run();
    Tool.of("greeter/inert@2", () -> greeter.install("2", folders.tools("greeter/inert@2"))).run();
  }

  @Override
  public ToolProvider install(String version, Path into) {
    var file = into.resolve("Prog.java");
    if (!Files.exists(file)) {
      try {
        Files.createDirectories(into);
        var text = // language=java
            """
            class Prog {
              public static void main(String... args) {
                System.out.println("Greetings! [%s]");
              }
            }
            """
                .formatted(version);
        Files.writeString(file, text);
        System.out.println("Wrote " + file.toUri());
      } catch (Exception exception) {
        throw new RuntimeException();
      }
    }
    return ToolProgram.java(file.toString());
  }
}
