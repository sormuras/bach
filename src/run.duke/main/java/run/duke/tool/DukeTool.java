package run.duke.tool;

import java.io.PrintWriter;
import java.util.Arrays;
import run.duke.Duke;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record DukeTool() implements Duke, ToolOperator {
  @Override
  public String name() {
    return "duke";
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      out.println("This is duke.");
      return 0;
    }
    return run(args[0], runner, out, err, Arrays.copyOfRange(args, 1, args.length));
  }

  int run(String tool, ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    return switch (tool) {
      case "check-java-release" -> new CheckJavaReleaseTool().run(out, err, args);
      case "check-java-version" -> new CheckJavaVersionTool().run(out, err, args);
      case "list" -> new ListTool(runner).run(out, err, args);
      case "tree" -> new TreeTool().run(out, err, args);
      default -> {
        err.println("Tool not found: " + tool);
        yield 1;
      }
    };
  }
}
