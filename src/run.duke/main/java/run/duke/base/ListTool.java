package run.duke.base;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.duke.ToolRunner;
import run.duke.CommandLineInterface;

public record ListTool(ToolRunner runner) implements ToolProvider {
  record Options(String topic, String... args) {
    enum Topic {
      tools
    }
  }

  @Override
  public String name() {
    return "list";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var topics = Options.Topic.values();
    if (args.length == 0) {
      try {
        out.println("Available topics");
        for (int i = 0; i < topics.length; i++) {
          out.printf("%2d -> %s%n", i, topics[i]);
        }
        var console = System.console();
        if (console == null) {
          err.println("No console available");
          return 1;
        }
        var choice = console.readLine("Your choice? ");
        if (choice == null) return 0;
        return run(topics[Integer.parseInt(choice)], out, err);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }

    var parser = CommandLineInterface.parser(MethodHandles.lookup(), Options.class);
    var options = parser.parse(args);
    try {
      var topic = Options.Topic.valueOf(options.topic);
      return run(topic, out, err, options.args);
    } catch (IllegalArgumentException exception) {
      err.println(options.topic + " is not a supported list topic. Available topics are:");
      Stream.of(topics).forEach(constant -> err.println("- " + constant.name()));
      return 1;
    }
  }

  int run(Options.Topic topic, PrintWriter out, PrintWriter err, String... args) {
    return switch (topic) {
      case tools -> listTools(out);
    };
  }

  int listTools(PrintWriter out) {
    var map = new TreeMap<String, List<String>>();
    var max = 0;
    for (var finder : runner.toolbox().finders()) {
      for (var identifier : finder.identifiers()) {
        var name = identifier.substring(identifier.lastIndexOf('/') + 1);
        map.computeIfAbsent(name, __ -> new ArrayList<>()).add(identifier);
        max = Math.max(max, name.length());
      }
    }
    var lines = new ArrayList<String>();
    for (var entry : map.entrySet()) {
      lines.add(("%" + max + "s -> %s").formatted(entry.getKey(), entry.getValue()));
    }
    var size = map.size();
    lines.add("    %d tool%s".formatted(size, size == 1 ? "" : "s"));
    out.println(String.join("\n", lines));
    return 0;
  }
}
