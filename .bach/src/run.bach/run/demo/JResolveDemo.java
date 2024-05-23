package run.demo;

import run.bach.*;

public class JResolveDemo {
  public static void main(String... args) {
    var runner = new ToolSpace(ToolSpace.Flag.SILENT);
    var jresolve = Tool.of("https://github.com/bowbahdoe/jresolve-cli/releases/download/v2024.05.10/jresolve.jar#SIZE=754432");
    var run =
        runner.run(
            jresolve,
            "pkg:maven/org.junit.jupiter/junit-jupiter-engine@5.10.2",
            "pkg:maven/org.junit.platform/junit-platform-console@1.10.2");
    System.out.println(run.out());
  }
}
