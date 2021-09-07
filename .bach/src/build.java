import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.ExternalModuleLocators;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.command.JarCommand;
import com.github.sormuras.bach.external.JUnit;
import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class build {

  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/logging.properties");
    System.out.println("BEGIN");
    try (var bach = new Bach(args)) {
      var version = version(bach);

      bach.logCaption("Grab required and missing external modules");
      var grabber = bach.grabber(locators());
      grabber.grabExternalModules(
          "org.junit.jupiter", "org.junit.platform.console", "org.junit.platform.jfr");
      grabber.grabMissingExternalModules();

      bach.logCaption("Grab external tools");
      bach.run("grab", grab -> grab.add(bach.path().root(".bach", "external.properties")));

      bach.logCaption("Build main code space");
      var mainModules = buildMainModules(bach, version);

      bach.logCaption("Build test code space");
      var testModules = buildTestModules(bach, version, mainModules);

      executeTests(bach, "com.github.sormuras.bach", mainModules, testModules);
      executeTests(bach, "test.base", mainModules, testModules);
      executeTests(bach, "test.integration", mainModules, testModules);
      executeTests(bach, "test.projects", mainModules, testModules);

      bach.logCaption("Generate documentation");
      generateApiDocumentation(bach, version);
    }
    System.out.println("END.");
  }

  static ExternalModuleLocators locators() {
    return ExternalModuleLocators.of(build::locate, JUnit.version("5.8.0-RC1"));
  }

  static String locate(String module) {
    return switch (module) {
      case "org.apiguardian.api" -> "https://repo.maven.apache.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar#SIZE=6806";
      case "org.junit.jupiter" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter/5.8.0-RC1/junit-jupiter-5.8.0-RC1.jar#SIZE=6374";
      case "org.junit.jupiter.api" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-api/5.8.0-RC1/junit-jupiter-api-5.8.0-RC1.jar#SIZE=192751";
      case "org.junit.jupiter.engine" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.8.0-RC1/junit-jupiter-engine-5.8.0-RC1.jar#SIZE=224308";
      case "org.junit.jupiter.params" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-params/5.8.0-RC1/junit-jupiter-params-5.8.0-RC1.jar#SIZE=575548";
      case "org.junit.platform.commons" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-commons/1.8.0-RC1/junit-platform-commons-1.8.0-RC1.jar#SIZE=100449";
      case "org.junit.platform.console" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console/1.8.0-RC1/junit-platform-console-1.8.0-RC1.jar#SIZE=488177";
      case "org.junit.platform.engine" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-engine/1.8.0-RC1/junit-platform-engine-1.8.0-RC1.jar#SIZE=185793";
      case "org.junit.platform.launcher" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.8.0-RC1/junit-platform-launcher-1.8.0-RC1.jar#SIZE=159616";
      case "org.junit.platform.reporting" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-reporting/1.8.0-RC1/junit-platform-reporting-1.8.0-RC1.jar#SIZE=26190";
      case "org.opentest4j" -> "https://repo.maven.apache.org/maven2/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar#SIZE=7653";
      default -> null;
    };
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
    var classes = Path.of(".bach/workspace/classes");
    bach.run(
        Command.javac()
            .release(17)
            .modules("com.github.sormuras.bach")
            .moduleSourcePathPatterns("./*/main/java")
            .add("-g")
            .add("-parameters")
            .add("-Werror")
            .add("-Xlint")
            .add("-encoding", "UTF-8")
            .add("-d", classes));
    var modules = Path.of(".bach/workspace/modules");
    bach.run(ToolCall.of("directories", "create", modules));
    var file = Configuration.computeJarFileName("com.github.sormuras.bach", version);
    bach.run(
        Command.jar()
            .mode("--create")
            .file(modules.resolve(file))
            .verbose(true)
            .add("--module-version", version)
            .main("com.github.sormuras.bach.Main")
            .filesAdd(classes.resolve("com.github.sormuras.bach"))
            .filesAdd(Path.of("com.github.sormuras.bach").resolve("main/java")));
    return modules;
  }

  static Path buildTestModules(Bach bach, Version version, Path mainModules) {
    var names =
        List.of("test.base", "test.integration", "test.projects", "com.github.sormuras.bach");
    var mainClasses = Path.of(".bach/workspace/classes");
    var testClasses = Path.of(".bach/workspace/test-classes");
    bach.run(
        Command.javac()
            .modules(names)
            .moduleSourcePathPatterns("./*/test/java", "./*/test/java-module")
            .add("--module-path", mainModules + File.pathSeparator + bach.path().externalModules())
            .add(
                "--patch-module",
                "com.github.sormuras.bach=" + mainClasses.resolve("com.github.sormuras.bach"))
            .add("-g")
            .add("-parameters")
            .add("-Werror")
            .add("-Xlint")
            .add("-encoding", "UTF-8")
            .add("-d", testClasses));
    var modules = Path.of(".bach/workspace/test-modules");
    bach.run(ToolCall.of("directories", "create", modules));
    var jars = new ArrayList<JarCommand>();
    for (var name : names) {
      var file = name + "@" + version + "+test.jar";
      var jar =
          Command.jar()
              .mode("--create")
              .file(modules.resolve(file))
              .add("--module-version", version + "+test")
              .filesAdd(testClasses.resolve(name));
      if (name.equals("com.github.sormuras.bach")) {
        jar = jar.filesAdd(mainClasses.resolve("com.github.sormuras.bach"));
      }
      var resources = Path.of(name, "test", "resources");
      if (Files.isDirectory(resources)) {
        jar = jar.filesAdd(resources);
      }
      jars.add(jar);
    }
    jars.parallelStream().forEach(bach::run);
    return modules;
  }

  static void executeTests(Bach bach, String module, Path mainModules, Path testModules) {
    bach.logCaption("Execute tests of module " + module);
    var moduleFinder =
        ModuleFinder.of(
            testModules.resolve(module + "@" + version(bach) + "+test.jar"),
            mainModules,
            testModules,
            bach.path().externalModules());
    var toolFinder = ToolFinder.of(moduleFinder, true, module);
    bach.run(
        ToolCall.of(
            toolFinder,
            Command.of("junit")
                .add("--select-module", module)
                .add("--reports-dir", Path.of(".bach/workspace/test-reports/junit-" + module))));
  }

  static void generateApiDocumentation(Bach bach, Version version) {
    var api = bach.path().workspace("documentation", "api");
    bach.run(
        Command.of("javadoc")
            .add("--module", "com.github.sormuras.bach")
            .add("--module-source-path", "./*/main/java")
            .add("-encoding", "UTF-8")
            .add("-windowtitle", "🎼 Bach " + version)
            .add("-header", "🎼 Bach %s API documentation".formatted(version))
            .add("-notimestamp")
            .add("-use")
            .add("-linksource")
            .add("-Xdoclint:-missing")
            .add("-Werror")
            .add("-d", api));
    bach.run(
        Command.jar()
            .mode("--create")
            .file(api.getParent().resolve("api.zip"))
            .add("--no-manifest")
            .filesAdd(api));
  }
}
