import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("all")
class BuildDemo02Testing {

  static final Path ROOT = Paths.get(".").normalize().toAbsolutePath();
  static final Path DEPS = Paths.get(".bach", "resolved");
  static final Path MAIN = Paths.get("target", "bach", "testing", "main");
  static final Path TEST = Paths.get("target", "bach", "testing", "test");

  public static void main(String... args) throws IOException {
    System.out.println("BuildDemo02Testing.main");
    System.out.println(ROOT);
    BuildDemo02Testing demo = new BuildDemo02Testing();
    demo.resolveRequiredModules();
    demo.clean();
    // main
    demo.compileMain();
    demo.run();
    // test
    // TODO demo.compileTest();
    // TODO demo.testOnClassPath();
    // TODO demo.testOnModulePath();
  }

  void clean() throws IOException {
    // TODO Bach.Basics.treeDelete(Paths.get("target", "bach"));
  }

  void resolveRequiredModules() {
    // official coordinates of released artifacts
    String jupiterVersion = "5.1.0";
    String platformVersion = "1.1.0";
    // TODO Bach.Basics.resolve("org.junit.jupiter", "junit-jupiter-api", jupiterVersion);
    // TODO Bach.Basics.resolve("org.junit.jupiter", "junit-jupiter-engine", jupiterVersion);
    // TODO Bach.Basics.resolve("org.junit.platform", "junit-platform-console", platformVersion);
    // TODO Bach.Basics.resolve("org.junit.platform", "junit-platform-commons", platformVersion);
    // TODO Bach.Basics.resolve("org.junit.platform", "junit-platform-engine", platformVersion);
    // TODO Bach.Basics.resolve("org.junit.platform", "junit-platform-launcher", platformVersion);
    /*
    // official coordinates of snapshot artifacts
    String jupiterVersion = "5.1.0-SNAPSHOT";
    String platformVersion = "1.1.0-SNAPSHOT";
    Bach.Basics.resolve("org.junit.jupiter", "junit-jupiter-api", jupiterVersion);
    Bach.Basics.resolve("org.junit.jupiter", "junit-jupiter-engine", jupiterVersion);
    Bach.Basics.resolve("org.junit.platform", "junit-platform-console", platformVersion);
    Bach.Basics.resolve("org.junit.platform", "junit-platform-commons", platformVersion);
    Bach.Basics.resolve("org.junit.platform", "junit-platform-engine", platformVersion);
    Bach.Basics.resolve("org.junit.platform", "junit-platform-launcher", platformVersion);
    */
    /*
    // jitpack.io coordinates of not-even-snapshot artifacts
    String group = "com.github.junit-team.junit5";
    String version = "jigsaw-SNAPSHOT";
    Bach.Basics.resolve(group, "junit-jupiter-api", version);
    Bach.Basics.resolve(group, "junit-jupiter-engine", version);
    Bach.Basics.resolve(group, "junit-platform-console", version);
    Bach.Basics.resolve(group, "junit-platform-commons", version);
    Bach.Basics.resolve(group, "junit-platform-engine", version);
    Bach.Basics.resolve(group, "junit-platform-launcher", version);
    */
    // 3rd-party modules
    // TODO Bach.Basics.resolve("org.opentest4j", "opentest4j", "1.0.0");
    // TODO Bach.Basics.resolve("org.apiguardian", "apiguardian-api", "1.0.0");
  }

  void compileMain() {
    var javac = new JdkTool.Javac();
    javac.destination = MAIN;
    javac.modulePath = List.of(DEPS);
    javac.moduleSourcePath = List.of(Paths.get("src", "main", "java"));
    javac.run();
  }

  void compileTest() {
    var tests = List.of(Paths.get("src", "test", "java"));
    var mains = List.of(Paths.get("src", "main", "java"));

    var javac = new JdkTool.Javac();
    javac.destination = TEST;
    javac.modulePath = List.of(DEPS);
    javac.moduleSourcePath = tests;
    // TODO javac.patchModule = Bach.Basics.getPatchMap(tests, mains);
    javac.run();
  }

  void run() {
    var java = new JdkTool.Java();
    java.modulePath = List.of(MAIN, DEPS);
    java.module = "application/application.Main";
    java.run();
  }

  void testOnClassPath() throws IOException {
    new Command("java")
        // TODO .add("--class-path")
        // TODO .add(Bach.Basics.getClassPath(List.of(TEST), List.of(DEPS)))
        .add("org.junit.platform.console.ConsoleLauncher")
        .add("--scan-class-path")
        .run();
  }

  void testOnModulePath() throws IOException {
    var java = new JdkTool.Java();
    java.modulePath = List.of(TEST, DEPS);
    java.addModules = List.of("ALL-MODULE-PATH", "ALL-DEFAULT");
    java.module = "org.junit.platform.console";
    java.args = List.of("--scan-modules");
    java.run();
  }
}
