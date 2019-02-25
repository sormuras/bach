import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

class Build {

  public static void main(String... args) throws Exception {
    if (main(new Bach(List.of(args))) != 0) {
      throw new Error("Build error!");
    }
  }

  public static int main(Bach bach) throws Exception {

    var format =
        new Bach.Tool.GoogleJavaFormat(
            Boolean.getBoolean("bach.format.replace"), Set.of(Path.of("src"), Path.of("demo")));

    var updateScaffoldArchive =
        new Bach.Action.ToolRunner(
            "jar",
            "--verbose",
            "--update",
            "--file",
            "demo/scaffold.zip",
            "--no-manifest",
            "-C",
            "demo/scaffold",
            ".");
    bach.utilities.treeDelete(Path.of("demo/scaffold/.bach"));
    bach.utilities.treeDelete(Path.of("demo/scaffold/bin"));

    var target = "target/build";

    var compileMain =
        new Bach.Action.ToolRunner("javac", "-d", target + "/main", "src/main/Bach.java");

    var junit =
        bach.utilities.download(
            Path.of(target), URI.create(bach.var.get(Bach.Property.TOOL_JUNIT_URI)));

    var compileTest =
        new Bach.Action.ToolRunner(
            new Bach.Command("javac")
                .add("-d")
                .add(target + "/test")
                .add("-cp")
                .add(List.of(Path.of(target + "/main"), junit))
                .addAllJavaFiles(List.of(Path.of("src/test"))));

    var test =
        new Bach.Action.ToolRunner(
            new Bach.Command("java")
                .add("-ea")
                .add("-cp")
                .add(List.of(Path.of(target + "/test"), Path.of(target + "/main"), junit))
                .add("org.junit.platform.console.ConsoleLauncher")
                .add("--scan-class-path"));

    bach.utilities.treeCopy(Path.of("src/test-resources"), Path.of(target + "/test"));

    var JAVADOC = target + "/javadoc";
    var ARTIFACTS = Path.of(target, "artifacts");

    Files.createDirectories(Path.of(JAVADOC));
    var javadoc =
        new Bach.Action.ToolRunner(
            new Bach.Command("javadoc")
                .add("-d")
                .add(JAVADOC)
                .add("-package")
                .add("-quiet")
                .add("-keywords")
                .add("-html5")
                .add("-linksource")
                .add("-Xdoclint:all,-missing")
                .add("-link")
                .add("https://docs.oracle.com/en/java/javase/11/docs/api/")
                .add("src/main/Bach.java"));

    //    bach.log.info("Package");
    Files.createDirectories(ARTIFACTS);
    var jarBinaries =
        new Bach.Tool.ToolRunner(
            new Bach.Command("jar")
                .add("--create")
                .add("--file")
                .add(ARTIFACTS.resolve("bach.jar"))
                .add("-C")
                .add(target + "/main")
                .add("."));

    var jarSources =
        new Bach.Tool.ToolRunner(
            new Bach.Command("jar")
                .add("--create")
                .add("--file")
                .add(ARTIFACTS.resolve("bach-sources.jar"))
                .add("-C")
                .add("src/main")
                .add("."));

    var jarJavadoc =
        new Bach.Tool.ToolRunner(
            new Bach.Command("jar")
                .add("--create")
                .add("--file")
                .add(ARTIFACTS.resolve("bach-javadoc.jar"))
                .add("-C")
                .add(JAVADOC)
                .add("."));

    //    bach.log.info("JDeps");
    //    new Command("jdeps")
    //            .add("-summary")
    //            .add("-recursive")
    //            .add(ARTIFACTS.resolve("bach.jar"))
    //            .run(bach);

    return bach.run(
        format,
        updateScaffoldArchive,
        compileMain,
        compileTest,
        test,
        javadoc,
        jarBinaries,
        jarSources,
        jarJavadoc);
  }
}
