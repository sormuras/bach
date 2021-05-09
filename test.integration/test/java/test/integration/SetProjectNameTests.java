package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SetProjectNameTests {
  @Test
  void viaDirectoryName(@TempDir Path temp) throws Exception {
    var name = "demo";
    var demo = Files.createDirectories(temp.resolve(name));

    var bach = Bach.of(errorLogbook(), chrootOptions(demo));
    assertEquals(name, bach.project().name());
  }

  @Test
  void viaFile(@TempDir Path temp) throws Exception {
    scaffold(temp, Map.of("bach.args", """
        --project-name
          demo
        """));

    var bach = Bach.of(errorLogbook(), chrootOptions(temp));
    assertEquals("demo", bach.project().name());
  }

  @Test
  void viaOption(@TempDir Path temp) {
    var name = "demo";

    var bach = Bach.of(errorLogbook(), chrootOptions(temp).with("projectName", Optional.of(name)));
    assertEquals(name, bach.project().name());
  }

  @Test
  void viaCommandLineArgument(@TempDir Path temp) {
    var name = "demo";
    var args = List.of("--chroot", temp.toString(), "--project-name", name);

    var bach = Bach.of(errorPrinter(), args.toArray(String[]::new));
    assertEquals(name, bach.project().name());
  }

  @Test
  void viaCommandLineArgumentInArgumentFile(@TempDir Path temp) throws Exception {
    scaffold(temp, Map.of("options", """
        --project-name
          demo
        """));
    var args = List.of("--chroot", temp.toString(), "@" + temp.resolve("options"));

    var bach = Bach.of(errorPrinter(), args.toArray(String[]::new));
    assertEquals("demo", bach.project().name());
  }

  @Test
  void viaProjectInfoNameElement(@TempDir Path temp) throws Exception {
    var info =
        """
        import com.github.sormuras.bach.api.*;

        @ProjectInfo(name = "demo")
        module bach.info {
          requires com.github.sormuras.bach;
        }
        """;
    scaffold(temp, Map.of(".bach/bach.info/module-info.java", info));

    var bach = Bach.of(errorLogbook(), chrootOptions(temp));
    assertEquals("demo", bach.project().name());
  }

  @Test
  void viaProjectInfoArgumentsAsStringArray(@TempDir Path temp) throws Exception {
    var info =
        """
        import com.github.sormuras.bach.api.*;

        @ProjectInfo(arguments = {"--project-name", "demo"})
        module bach.info {
          requires com.github.sormuras.bach;
        }
        """;
    scaffold(temp, Map.of(".bach/bach.info/module-info.java", info));

    var bach = Bach.of(errorLogbook(), chrootOptions(temp));
    assertEquals("demo", bach.project().name());
  }

  @Test
  void viaProjectInfoArgumentsAsTextBlock(@TempDir Path temp) throws Exception {
    var info =
        """
        import com.github.sormuras.bach.api.*;

        @ProjectInfo(arguments =
          ""\"
          --project-name
            demo
          ""\")
        module bach.info {
          requires com.github.sormuras.bach;
        }
        """;
    scaffold(temp, Map.of(".bach/bach.info/module-info.java", info));

    var bach = Bach.of(errorLogbook(), chrootOptions(temp));
    assertEquals("demo", bach.project().name());
  }

  static Printer errorPrinter() {
    var out = new PrintWriter(Writer.nullWriter());
    var err = new PrintWriter(System.err, true);
    return new Printer(out, err);
  }

  static Logbook errorLogbook() {
    return Logbook.of(errorPrinter(), true);
  }

  static Options chrootOptions(Path path) {
    return Options.of().id("Test Options").with("chroot", Optional.of(path));
  }

  static void scaffold(Path root, Map<String, String> files) throws Exception {
    for (var file : files.entrySet()) {
      var path = root.resolve(file.getKey());
      Files.createDirectories(path.getParent());
      Files.writeString(path, file.getValue());
    }
  }
}
