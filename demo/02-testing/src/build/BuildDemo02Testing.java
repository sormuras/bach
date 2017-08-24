import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
    javac.patchModule = Basics.getPatchMap(tests, Paths.get("src", "main", "java"));
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
        .add(Basics.getClassPath(List.of(TEST), List.of(DEPS)))
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
    ModuleFinder.of(TEST)
        .findAll()
        .forEach(reference -> command.add("--class-path").add(Basics.getPath(reference)));
    command.run();
  }
}
