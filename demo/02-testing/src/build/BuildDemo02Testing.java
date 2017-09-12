import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("all")
class BuildDemo02Testing {

  static final Path ROOT = Paths.get(".").normalize().toAbsolutePath();
  static final Path DEPS = Paths.get(".bach", "resolved");
  static final Path MAIN = Paths.get("target", "bach", "main");
  static final Path TEST = Paths.get("target", "bach", "test");

  public static void main(String... args) throws IOException {
    System.out.println("BuildDemo02Testing.main");
    System.out.println(ROOT);
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
    Bach.Basics.treeDelete(Paths.get("target", "bach"));
  }

  void resolveRequiredModules() {
    Bach.Basics.resolve("org.junit.jupiter", "junit-jupiter-api", "5.0.0");
    Bach.Basics.resolve("org.junit.jupiter", "junit-jupiter-engine", "5.0.0");
    Bach.Basics.resolve("org.junit.platform", "junit-platform-console", "1.0.0");
    Bach.Basics.resolve("org.junit.platform", "junit-platform-commons", "1.0.0");
    Bach.Basics.resolve("org.junit.platform", "junit-platform-engine", "1.0.0");
    Bach.Basics.resolve("org.junit.platform", "junit-platform-launcher", "1.0.0");
    Bach.Basics.resolve("org.opentest4j", "opentest4j", "1.0.0");
    Bach.Basics.resolve("org.apiguardian", "apiguardian-api", "1.0.0");
  }

  void compileMain() {
    Bach.JdkTool.Javac javac = new Bach.JdkTool.Javac();
    javac.destinationPath = MAIN;
    javac.modulePath = List.of(DEPS);
    javac.moduleSourcePath = List.of(Paths.get("src", "main", "java"));
    javac.run();
  }

  void compileTest() {
    List<Path> tests = List.of(Paths.get("src", "test", "java"));
    List<Path> mains = List.of(Paths.get("src", "main", "java"));

    Bach.JdkTool.Javac javac = new Bach.JdkTool.Javac();
    javac.destinationPath = TEST;
    javac.modulePath = List.of(DEPS);
    javac.moduleSourcePath = tests;
    javac.patchModule = Bach.Basics.getPatchMap(tests, mains);
    javac.run();
  }

  void run() {
    Bach.JdkTool.Java java = new Bach.JdkTool.Java();
    java.modulePath = List.of(MAIN, DEPS);
    java.module = "application/application.Main";
    java.run();
  }

  void testOnClassPath() throws IOException {
    new Bach.Command("java")
        .add("--class-path")
        .add(Bach.Basics.getClassPath(List.of(TEST), List.of(DEPS)))
        .add("org.junit.platform.console.ConsoleLauncher")
        .add("--scan-class-path")
        .run();
  }

  void testOnModulePath() throws IOException {
    Bach.JdkTool.Java java = new Bach.JdkTool.Java();
    java.modulePath = List.of(TEST, DEPS);
    java.module = "org.junit.platform.console";
    Bach.Command command = java.toCommand();
    command.add("--scan-class-path");
    ModuleFinder.of(TEST)
        .findAll()
        .forEach(mr -> command.add("--class-path").add(ROOT.relativize(Bach.Basics.getPath(mr))));
    command.run();
  }
}
