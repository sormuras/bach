package run;

import run.bach.*;
import run.bach.external.*;

class ToolSpaceDemo extends ToolSpace {
  public static void main(String... args) {
    var finder = ToolFinder.ofInstaller().with(new Maven("3.9.6"));
    var space = new ToolSpaceDemo(finder);

    var run = space.run("maven", "--version");

    run.out().lines().filter(line -> line.startsWith("Apache Maven")).forEach(System.out::println);
    if (run.code() != 0) throw new Error("Non-zero error code: " + run.code());
  }

  ToolSpaceDemo(ToolFinder finder) {
    super(finder);
  }

  @Override
  protected void announce(ToolCall call) {
    System.out.println("BEGIN -> " + call.tool().name());
  }

  @Override
  protected void verify(ToolRun run) {
    System.out.println("<-- END. " + run.call().tool().name());
  }
}
