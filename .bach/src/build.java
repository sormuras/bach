import static com.github.sormuras.bach.Note.caption;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.ToolFinder;
import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class build {

  static final Path EXTERNAL_MODULES = Path.of(".bach", "external-modules");

  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/logging.properties");
    System.out.println("BEGIN");
    try (var bach = new Bach(args)) {
      var version = version(bach);

      bach.log(caption("Restore external assets"));
      bach.run(Call.tool("restore", ".bach/external.properties"));

      bach.log(caption("Build main code space"));
      var mainModules = buildMainModules(bach, version);

      bach.log(caption("Build test code space"));
      var testModules = buildTestModules(bach, version, mainModules);

      executeTests(bach, "com.github.sormuras.bach", mainModules, testModules);
      executeTests(bach, "test.base", mainModules, testModules);
      executeTests(bach, "test.integration", mainModules, testModules);
      executeTests(bach, "test.projects", mainModules, testModules);
    }
    System.out.println("END.");
  }

  static Version version(Bach bach) {
    var version = bach.configuration().projectOptions().version();
    if (version.isPresent()) return version.get();
    var file = Path.of("VERSION");
    try {
      return Version.parse(Files.readString(file) + "-ea");
    } catch (Exception exception) {
      throw new RuntimeException("Reading VERSION file failed: " + file.toUri(), exception);
    }
  }

  static Path buildMainModules(Bach bach, Version version) {
    var names = List.of("com.github.sormuras.bach");
    var classes = Path.of(".bach/workspace/classes");
    bach.run(
            Call.tool("javac")
                .with("--release", "17")
                .with("--module", String.join(",", names))
                .with("--module-source-path", "./*/main/java")
                .with("-g")
                .with("-parameters")
                .with("-Werror")
                .with("-Xlint")
                .with("-encoding", "UTF-8")
                .with("-d", classes));
    var modules = Path.of(".bach/workspace/modules");
    bach.run(Call.tool("directories", "create", modules));
    for (var name : names) {
      var file = name + "@" + version + ".jar";
      bach.run(
              Call.tool("jar")
                  .with("--verbose")
                  .with("--create")
                  .with("--file", modules.resolve(file))
                  .with("--module-version", version)
                  .with("-C", classes.resolve(name).toString(), ".")
                  .with("-C", Path.of(name).resolve("main/java").toString(), "."));
    }
    return modules;
  }

  static Path buildTestModules(Bach bach, Version version, Path mainModules) {
    var names =
        List.of("test.base", "test.integration", "test.projects", "com.github.sormuras.bach");
    var classes = Path.of(".bach/workspace/test-classes");
    bach.run(
            Call.tool("javac")
                .with("--module", String.join(",", names))
                .with(
                    "--module-source-path",
                    String.join(File.pathSeparator, "./*/test/java", "./*/test/java-module"))
                .with("--module-path", List.of(mainModules, EXTERNAL_MODULES))
                .with(
                    "--patch-module", "com.github.sormuras.bach=com.github.sormuras.bach/main/java")
                .with("-g")
                .with("-parameters")
                .with("-Werror")
                .with("-Xlint")
                .with("-encoding", "UTF-8")
                .with("-d", classes));
    var modules = Path.of(".bach/workspace/test-modules");
    bach.run(Call.tool("directories", "create", modules));
    for (var name : names) {
      var file = name + "@" + version + "+test.jar";
      var jar =
          Call.tool("jar")
              .with("--create")
              .with("--file", modules.resolve(file))
              .with("--module-version", version + "+test")
              .with("-C", classes.resolve(name).toString(), ".");
      var resources = Path.of(name, "test", "resources");
      if (Files.isDirectory(resources)) {
        jar = jar.with("-C", resources, ".");
      }
      bach.run(jar);
    }
    return modules;
  }

  static void executeTests(Bach bach, String module, Path mainModules, Path testModules) {
    bach.log(caption("Execute tests of module " + module));
    var moduleFinder =
        ModuleFinder.of(
            testModules.resolve(module + "@" + version(bach) + "+test.jar"),
            mainModules,
            testModules,
            EXTERNAL_MODULES);
    var toolFinder = ToolFinder.of(moduleFinder, true, module);
    bach.run(
            Call.tool(toolFinder, "junit")
                .with("--select-module", module)
                .with("--reports-dir", Path.of(".bach/workspace/test-reports/junit-" + module)));
  }
}
