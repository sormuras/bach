package run.duke.tool;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Stream;
import run.duke.Duke;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolFinder;
import run.duke.ToolLogger;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record DukeTool() implements Duke, ToolOperator {
  @Override
  public String name() {
    return "duke";
  }

  record Options(String name, String... args) {}

  @Override
  public int run(ToolRunner runner, ToolLogger logger, String... args) {
    var finder =
        ToolFinder.of(
            Tool.of(new CheckJavaReleaseTool()),
            Tool.of(new CheckJavaVersionTool()),
            Tool.of(new ListTool(runner)),
            Tool.of(new TreeTool()));
    if (args.length == 0 || Set.of("--help", "-help", "-h", "/?", "?").contains(args[0])) {
      var out = logger.out();
      out.println("This is duke.");
      finder.tools().forEach(tool -> out.println(" -> " + tool.nickname()));
      return 0;
    }
    var options = Duke.splitter(MethodHandles.lookup(), Options.class).split(args);
    var found = finder.findTool(options.name()).orElseThrow();
    var call = ToolCall.of(found.provider()).with(Stream.of(options.args()));
    runner.run(call);
    return 0;
  }
}
