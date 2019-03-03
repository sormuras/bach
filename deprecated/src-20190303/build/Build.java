import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class Build {

  public static void main(String[] args) throws Exception {
    build(new Bach());
  }

  /** Called by {@code build.jsh} to get an exit value instead. */
  public static int main() {
    try {
      main(new String[0]);
    } catch (Throwable t) {
      t.printStackTrace();
      return 1;
    }
    return 0;
  }

  public static void build(Bach bach) throws Exception {
    bach.level = System.Logger.Level.ALL;

    System.out.println("[format]");
    bach.format(Boolean.getBoolean("bach.format.replace"), Path.of("src"), Path.of("demo"));

    var target = "target/build";
    bach.treeDelete(Path.of(target).resolve("artifacts"));
    bach.treeDelete(Path.of(target).resolve("javadoc"));
    bach.treeDelete(Path.of(target).resolve("main"));
    bach.treeDelete(Path.of(target).resolve("test"));

    System.out.println("[main]");
    bach.run("javac", "-d", target + "/main", "src/bach/Bach.java");

    System.out.println("[test]");
    var junitJar = Bach.Tool.JUnit.install(bach);

    new Bach.Command("javac", "-d", target + "/test")
        .add("--class-path")
        .add(List.of(Path.of(target, "main"), junitJar))
        .addAllJavaFiles(List.of(Path.of("src/test")))
        .execute(bach);
    bach.treeCopy(Path.of("src/test-resources"), Path.of(target + "/test"));

    bach.treeWalk(Path.of(target), System.out::println);
    bach.run(
        "java",
        "-ea",
        "-Djunit.jupiter.execution.parallel.enabled=true",
        "-Djunit.jupiter.execution.parallel.mode.default=concurrent",
        "--class-path",
        target + "/test" + File.pathSeparator + target + "/main" + File.pathSeparator + junitJar,
        "org.junit.platform.console.ConsoleLauncher",
        "--scan-class-path");

    var JAVADOC = target + "/javadoc";
    var ARTIFACTS = Path.of(target, "artifacts");

    System.out.println("[document]");
    Files.createDirectories(Path.of(JAVADOC));
    bach.run(
        "javadoc",
        "-d",
        JAVADOC,
        "-package",
        "-quiet",
        "-keywords",
        "-html5",
        "-linksource",
        "-Xdoclint:all,-missing",
        "-link",
        "https://docs.oracle.com/en/java/javase/11/docs/api/",
        "src/bach/Bach.java");

    System.out.println("[jar]");
    Files.createDirectories(ARTIFACTS);
    bach.run(
        "jar",
        "--create",
        "--file",
        ARTIFACTS.resolve("bach.jar").toString(),
        "--main-class",
        "Bach",
        "-C",
        target + "/main",
        ".");
    bach.run(
        "jar",
        "--create",
        "--file",
        ARTIFACTS.resolve("bach-sources.jar").toString(),
        "-C",
        "src/bach",
        ".");
    bach.run(
        "jar",
        "--create",
        "--file",
        ARTIFACTS.resolve("bach-javadoc.jar").toString(),
        "-C",
        JAVADOC,
        ".");
    bach.treeWalk(ARTIFACTS, System.out::println);

    System.out.println("[jdeps]");
    bach.run("jdeps", "-summary", "-recursive", ARTIFACTS.resolve("bach.jar").toString());

    System.out.println("[bach.jar banner]");
    bach.run("java", "-jar", ARTIFACTS.resolve("bach.jar").toString(), "banner");

    System.out.println("[demos]");
    bach.treeDelete(Path.of("demo/scaffold/.bach"));
    bach.treeDelete(Path.of("demo/scaffold/bin"));
    bach.run(
        "jar",
        "--update",
        "--file",
        "demo/scaffold.zip",
        "--no-manifest",
        "-C",
        "demo/scaffold",
        ".");
  }
}
