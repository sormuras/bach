import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@SuppressWarnings("all")
class BuildDemo02Testing {

  final Path DEPS = Paths.get(".bach", "resolved");
  final Path MAIN = Paths.get("target", "bach", "main");
  final Path TEST = Paths.get("target", "bach", "test");

  public static void main(String... args) throws IOException {
    System.out.println("BuildDemo02Testing.main");
    System.out.println(Paths.get(".").normalize().toAbsolutePath());
    System.setProperty("bach.verbose", "true");
    BuildDemo02Testing demo = new BuildDemo02Testing();
    demo.resolveRequiredModules();
    demo.clean();
    demo.compileMain();
    demo.compileTest();
    demo.run();
    demo.testOnClassPath();
    demo.testOnModulePath();
  }

  void clean() throws IOException {
    Basics.treeDelete(Paths.get("target", "bach"));
  }

  void resolveRequiredModules() {
    Basics.resolve("org.junit.jupiter", "junit-jupiter-api", "5.0.0-RC3");
    Basics.resolve("org.junit.jupiter", "junit-jupiter-engine", "5.0.0-RC3");
    Basics.resolve("org.junit.platform", "junit-platform-console", "1.0.0-RC3");
    Basics.resolve("org.junit.platform", "junit-platform-commons", "1.0.0-RC3");
    Basics.resolve("org.junit.platform", "junit-platform-engine", "1.0.0-RC3");
    Basics.resolve("org.junit.platform", "junit-platform-launcher", "1.0.0-RC3");
    Basics.resolve("org.opentest4j", "opentest4j", "1.0.0-RC1");
  }

  void compileMain() {
    JdkTool.Javac javac = new JdkTool.Javac();
    javac.destinationPath = MAIN;
    javac.modulePath = List.of(DEPS);
    javac.moduleSourcePath = List.of(Paths.get("src", "main", "java"));
    javac.run();
  }

  void compileTest() {
    Path tests = Paths.get("src", "test", "java");

    JdkTool.Javac javac = new JdkTool.Javac();
    javac.destinationPath = TEST;
    javac.modulePath = List.of(DEPS);
    javac.moduleSourcePath = List.of(tests);
    javac.patchModule = createPatchMap(tests, Paths.get("src", "main", "java"));
    javac.run();
  }

  void run() {
    JdkTool.Java java = new JdkTool.Java();
    java.modulePath = List.of(MAIN, DEPS);
    java.module = "application/application.Main";
    java.run();
  }

  void testOnClassPath() throws IOException {
    new JdkTool.Command("java")
        .add("--class-path")
        .add(createClassPath(List.of(TEST), List.of(DEPS)))
        .add("org.junit.platform.console.ConsoleLauncher")
        .add("--scan-class-path")
        .run();
  }

  void testOnModulePath() throws IOException {
    JdkTool.Java java = new JdkTool.Java();
    java.modulePath = List.of(TEST, DEPS);
    java.module = "org.junit.platform.console";
    JdkTool.Command command = java.toCommand();
    command.add("--scan-class-path");
    ModuleFinder.of(TEST).findAll().forEach(mr -> command.add("--class-path").add(asPath(mr)));
    command.run();
  }

  // TODO Move to Basics
  Path asPath(ModuleReference moduleReference) {
    return Paths.get(moduleReference.location().orElseThrow(AssertionError::new));
  }

  // TODO Move to Basics
  List<Path> createClassPath(List<Path> modulePaths, List<Path> depsPaths) {
    List<Path> classPath = new ArrayList<>();
    for (Path path : modulePaths) {
      ModuleFinder.of(path).findAll().forEach(this::asPath);
    }
    for (Path path : depsPaths) {
      try (Stream<Path> paths = Files.walk(path, 1)) {
        paths.filter(Basics::isJarFile).forEach(classPath::add);
      } catch (IOException e) {
        throw new AssertionError("failed adding jar(s) from " + path + " to the classpath", e);
      }
    }
    return classPath;
  }

  // TODO Move to Basics
  Map<String, List<Path>> createPatchMap(Path testModuleSourcePath, Path mainModuleSourcePath) {
    Map<String, List<Path>> map = new TreeMap<>();
    Basics.findDirectoryNames(testModuleSourcePath)
        .forEach(
            name -> {
              Path mainModule = mainModuleSourcePath.resolve(name);
              if (Files.exists(mainModule)) {
                map.put(name, List.of(mainModule));
              }
            });
    return map;
  }
}
