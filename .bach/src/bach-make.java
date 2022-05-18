import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.ToolRunner;
import com.github.sormuras.bach.project.ExternalModuleLocator;
import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class make {

  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/logging.properties");

    System.out.println("BEGIN");
    var bach = Bach.ofDefaults().with(ToolFinder.ofNativeToolsInJavaHome("java"));
    var version = version(bach);

    bach.run("banner", "Load required and missing external modules");
    var libs = Path.of(".bach", "external-modules");
    externalModules()
        .forEach((module, uri) -> bach.run("load-and-verify", libs.resolve(module + ".jar"), uri));

    bach.run("banner", "Build main code space");
    var mainModules = buildMainModules(bach, version);

    bach.run("bach", "--help");

    bach.run("banner", "Build test code space");
    var testModules = buildTestModules(bach, version, mainModules);

    executeTests(bach, "test.base", mainModules, testModules);
    executeTests(bach, "test.integration", mainModules, testModules);
    executeTestsInOtherVM(bach, "com.github.sormuras.bach", mainModules, testModules);

    bach.run("banner", "Generate documentation");
    generateApiDocumentation(bach, version);

    System.out.println("END.");
  }

  static Map<String, String> externalModules() {
    var map =
        new TreeMap<>(
            ExternalModuleLocator.SormurasBachExternalModulesProperties.of("junit", "5.9.0-M1")
                .findAll());
    map.remove("org.junit.vintage.engine"); // Exclude JUnit 4
    return map;
  }

  static Version version(Bach bach) {
    var file = Path.of("VERSION");
    try {
      return Version.parse(Files.readString(file) + "-ea");
    } catch (Exception exception) {
      throw new RuntimeException("Reading VERSION file failed: " + file.toUri(), exception);
    }
  }

  static Path buildMainModules(Bach bach, Version version) {
    var classes = Path.of(".bach/out/classes");
    bach.run(
        ToolCall.of("javac")
            .with("--release", 17)
            .with("--module", "com.github.sormuras.bach")
            .with("--module-source-path", "./*/src/main/java")
            .with("-g")
            .with("-parameters")
            .with("-Werror")
            .with("-Xlint")
            .with("-encoding", "UTF-8")
            .with("-d", classes));
    var modules = Path.of(".bach/out/modules");
    bach.run(ToolCall.of("tree", "--mode=CREATE", modules));
    var file = "com.github.sormuras.bach@" + version + ".jar";
    bach.run(
        ToolCall.of("jar")
            .with("--verbose")
            .with("--create")
            .with("--file", modules.resolve(file))
            .with("--module-version", version)
            .with("--main-class", "com.github.sormuras.bach.Main")
            .with("-C", classes.resolve("com.github.sormuras.bach"), ".")
            .withFindFiles(Path.of("com.github.sormuras.bach").resolve("src/main/java"), "**/*"));
    return modules;
  }

  static Path buildTestModules(Bach bach, Version version, Path mainModules) {
    var names = List.of("test.base", "test.integration", "com.github.sormuras.bach");
    var mainClasses = Path.of(".bach/out/classes");
    var testClasses = Path.of(".bach/out/test-classes");
    bach.run(
        ToolCall.of("javac")
            .with("--module", String.join(",", names))
            .with(
                "--module-source-path",
                "./*/src/test/java" + File.pathSeparator + "./*/src/test/java-module")
            .with(
                "--module-path",
                Stream.of(mainModules, Path.of(".bach", "external-modules"))
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator)))
            .with(
                "--patch-module",
                "com.github.sormuras.bach=" + mainClasses.resolve("com.github.sormuras.bach"))
            .with("-g")
            .with("-parameters")
            .with("-Werror")
            .with("-Xlint")
            .with("-encoding", "UTF-8")
            .with("-d", testClasses));
    var modules = Path.of(".bach/out/test-modules");
    bach.run(ToolCall.of("tree", "--mode=CREATE", modules));
    var jars = new ArrayList<ToolCall>();
    for (var name : names) {
      var file = name + "@" + version + "+test.jar";
      var jar =
          ToolCall.of("jar")
              .with("--create")
              .with("--file", modules.resolve(file))
              .with("--module-version", version + "+test")
              .with("-C", testClasses.resolve(name), ".");
      if (name.equals("com.github.sormuras.bach")) {
        jar = jar.with("-C", mainClasses.resolve("com.github.sormuras.bach"), ".");
      }
      var resources = Path.of(name, "test", "resources");
      if (Files.isDirectory(resources)) {
        jar = jar.with("-C", resources, ".");
      }
      jars.add(jar);
    }
    jars.parallelStream().forEach(bach::run);
    return modules;
  }

  static void executeTests(Bach bach, String module, Path mainModules, Path testModules) {
    bach.run("banner", "Execute tests of module " + module);
    var moduleFinder =
        ModuleFinder.of(
            testModules.resolve(module + "@" + version(bach) + "+test.jar"),
            mainModules,
            testModules,
            Path.of(".bach", "external-modules"));
    bach.run(
        ToolFinder.of(moduleFinder, true, module),
        ToolCall.of("junit")
            .with("--select-module", module)
            .with("--reports-dir", Path.of(".bach/out/test-reports/junit-" + module)),
        ToolRunner.RunModifier.RUN_WITH_PROVIDERS_CLASS_LOADER);
  }

  static void executeTestsInOtherVM(Bach bach, String module, Path mainModules, Path testModules) {
    bach.run("banner", "Execute tests of module " + module);
    bach.run(
        ToolCall.of("java")
            .with("-enableassertions")
            .with(
                "--module-path",
                Stream.of(
                        testModules.resolve(module + "@" + version(bach) + "+test.jar"),
                        mainModules,
                        testModules,
                        Path.of(".bach", "external-modules"))
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator)))
            .with(
                "--patch-module",
                "com.github.sormuras.bach="
                    + mainModules.resolve("com.github.sormuras.bach@" + version(bach) + ".jar"))
            .with("--module", "org.junit.platform.console")
            .with("--select-module", module)
            .with("--reports-dir", Path.of(".bach/out/test-reports/junit-" + module)));
  }

  static void generateApiDocumentation(Bach bach, Version version) {
    var api = Path.of(".bach", "out", "documentation", "api");
    bach.run(
        ToolCall.of("javadoc")
            .with("--module", "com.github.sormuras.bach")
            .with("--module-source-path", "./*/src/main/java")
            .with("-encoding", "UTF-8")
            .with("-windowtitle", "🎼 Bach " + version)
            .with("-header", "🎼 Bach %s API documentation".formatted(version))
            .with("-notimestamp")
            .with("-use")
            .with("-linksource")
            .with("-Xdoclint:-missing")
            .with("-Werror")
            .with("-d", api));
    bach.run(
        ToolCall.of("jar")
            .with("--create")
            .with("--file", api.getParent().resolve("api.zip"))
            .with("--no-manifest")
            .with("-C", api, "."));
  }
}
