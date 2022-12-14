package run.duke.tool;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import run.duke.Duke;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record DukeTool(ToolRunner runner) implements Duke, ToolFinder, ToolOperator, ToolProvider {
  public DukeTool() {
    this(ToolRunner.nullRunner());
  }

  @Override
  public Optional<String> description() {
    return Optional.of("Duke Tool Finder");
  }

  @Override
  public List<Tool> findTools() {
    return List.of(new Tool.OfOperator("run.duke/" + name(), this));
  }

  @Override
  public String name() {
    return "duke";
  }

  @Override
  public ToolProvider provider(ToolRunner runner) {
    return new DukeTool(runner);
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
