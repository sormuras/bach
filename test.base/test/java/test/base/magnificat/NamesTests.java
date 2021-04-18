package test.base.magnificat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.magnificat.api.Option;
import test.base.magnificat.api.ProjectInfo;

class NamesTests {
  @Test
  void projectNameViaDefaultConstant(@TempDir Path temp) {
    var name = ProjectInfo.DEFAULT_PROJECT_NAME;

    var bach = Bach.of(Printer.ofErrors(), chroot(temp));
    assertEquals(name, bach.project().name());
  }

  @Test
  void projectNameViaOption(@TempDir Path temp) {
    var name = "demo";

    var bach = Bach.of(Printer.ofErrors(), chroot(temp).with(Option.PROJECT_NAME, name));
    assertEquals(name, bach.project().name());
  }

  @Test
  void projectNameViaCommandLineArgument(@TempDir Path temp) {
    var name = "demo";
    var args = List.of(Option.CLI_BACH_ROOT.cli(), temp.toString(), "--project-name", name);

    var bach = Bach.of(Printer.ofErrors(), args.toArray(String[]::new));
    assertEquals(name, bach.project().name());
  }

  @Test
  @Disabled
  void projectNameViaProjectInfoNameElement(@TempDir Path temp) throws Exception {
    var info =
        """
        import test.base.magnificat.api.*;

        @ProjectInfo(name = "demo")
        module bach.info {
          requires test.base;
        }
        """;
    scaffold(temp, Map.of(".bach/bach.info/module-info.java", info));

    var bach = Bach.of(Printer.ofErrors(), chroot(temp));
    assertEquals("demo", bach.project().name());
  }

  @Test
  @Disabled
  void projectNameViaProjectInfoArgumentsAsStringArray(@TempDir Path temp) throws Exception {
    var info =
        """
        import test.base.magnificat.api.*;

        @ProjectInfo(arguments = {"--project-name", "demo"})
        module bach.info {
          requires test.base;
        }
        """;
    scaffold(temp, Map.of(".bach/bach.info/module-info.java", info));

    var bach = Bach.of(Printer.ofErrors(), chroot(temp));
    assertEquals("demo", bach.project().name());
  }

  @Test
  @Disabled
  void projectNameViaProjectInfoArgumentsAsTextBlock(@TempDir Path temp) throws Exception {
    var info =
        """
        import test.base.magnificat.api.*;

        @ProjectInfo(arguments =
          ""\"
          --project-name
            demo
          ""\")
        module bach.info {
          requires test.base;
        }
        """;
    scaffold(temp, Map.of(".bach/bach.info/module-info.java", info));

    var bach = Bach.of(Printer.ofErrors(), chroot(temp));
    assertEquals("demo", bach.project().name());
  }

  @Test
  @Disabled
  void projectNameViaProjectInfoOptionsProperties(@TempDir Path temp) throws Exception {
    var info =
        """
        import test.base.magnificat.api.*;
        import test.base.magnificat.api.ProjectInfo.*;

        @ProjectInfo(options = @Options(properties = @Property(option = Option.PROJECT_NAME, value = "demo")))
        module bach.info {
          requires test.base;
        }
        """;
    scaffold(temp, Map.of(".bach/bach.info/module-info.java", info));

    var bach = Bach.of(Printer.ofErrors(), chroot(temp));
    assertEquals("demo", bach.project().name());
  }

  static Options chroot(Path path) {
    return Options.of(Option.CLI_BACH_ROOT, path);
  }

  static void scaffold(Path root, Map<String, String> files) throws Exception {
    for (var file : files.entrySet()) {
      var path = root.resolve(file.getKey());
      Files.createDirectories(path.getParent());
      Files.writeString(path, file.getValue());
    }
  }
}
