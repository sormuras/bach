package run.duke;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.spi.ToolProvider;
import run.duke.tool.CheckJavaReleaseTool;
import run.duke.tool.CheckJavaVersionTool;
import run.duke.tool.ListTool;
import run.duke.tool.TreeTool;

public record DukeTool(ToolRunner runner) implements ToolProvider {
  public static ToolCall listTools() {
    return ToolCall.of("duke", "list", "tools");
  }

  public static ToolCall treeDelete(Path start) {
    return ToolCall.of("duke", "tree", "--mode=DELETE", start);
  }

  @Override
  public String name() {
    return "duke";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      out.println("This is duke.");
      return 0;
    }
    return run(args[0], out, err, Arrays.copyOfRange(args, 1, args.length));
  }

  int run(String tool, PrintWriter out, PrintWriter err, String... args) {
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
