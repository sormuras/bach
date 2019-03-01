import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class Build {

  public static void main(String[] args) throws Exception {
    build(new Bach());
  }

  public static int main() {
    try {
      build(new Bach());
    } catch (Throwable t) {
      t.printStackTrace();
      return 1;
    }
    return 0;
  }

  public static void build(Bach bach) throws Exception {
    bach.level = System.Logger.Level.ALL;

    var userHome = Path.of(System.getProperty("user.home"));
    var formatUri =
        URI.create(
            "https://github.com/"
                + "google/google-java-format/releases/download/google-java-format-1.7/"
                + "google-java-format-1.7-all-deps.jar");
    Files.createDirectories(userHome.resolve(".bach/tools/google-java-format"));
    var format = bach.download(userHome.resolve(".bach/tools/google-java-format"), formatUri);

    // TODO "--dry-run", "--set-exit-if-changed"
    new Bach.Command("java", "-jar", format.toString(), "--replace")
        .addAllJavaFiles(List.of(Path.of("src"), Path.of("demo")))
        .execute(bach);

    var target = "target/build";
    bach.treeDelete(Path.of(target).resolve("artifacts"));
    bach.treeDelete(Path.of(target).resolve("javadoc"));
    bach.treeDelete(Path.of(target).resolve("main"));
    bach.treeDelete(Path.of(target).resolve("test"));

    System.out.println("[main]");
    bach.run("javac", "-d", target + "/main", "src/bach/Bach.java");

    System.out.println("[test]");
    var junitUri =
        URI.create(
            "http://central.maven.org/"
                + "maven2/org/junit/platform/junit-platform-console-standalone/1.4.0/"
                + "junit-platform-console-standalone-1.4.0.jar");
    var junit = bach.download(Path.of(target), junitUri);

    new Bach.Command("javac", "-d", target + "/test")
        .add("--class-path")
        .add(List.of(Path.of(target, "main"), junit))
        .addAllJavaFiles(List.of(Path.of("src/test")))
        .execute(bach);
    bach.treeCopy(Path.of("src/test-resources"), Path.of(target + "/test"));

    bach.treeWalk(Path.of(target), System.out::println);
    bach.run(
        "java",
        "-ea",
        "--class-path",
        target + "/test" + File.pathSeparator + target + "/main" + File.pathSeparator + junit,
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
