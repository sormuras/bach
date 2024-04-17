package run;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.spi.*;
import run.bach.*;
import run.bach.external.*;

class ToolFinderDemo {
  public static void main(String... args) {
    var finder =
        ToolFinder.compose(
            // ToolProvider SPI, i.e. ToolProvider.findFirst(NAME)
            ToolFinder.of(Tool.of("jar"), Tool.of("javac")),
            // Native executable program in ${JAVA_HOME}/bin/NAME[.exe]
            ToolFinder.of(Tool.of("java"), Tool.of("jfr")),
            // ToolProvider instance
            ToolFinder.of(
                Tool.of(new Noop("noop", 0)),
                Tool.of(new Noop("fail", 1)),
                Tool.of(new RunnableTool("flush", System::gc)),
                Tool.of(new CallableTool("processors", Runtime.getRuntime()::availableProcessors)),
                Tool.of(new ConsumerTool("consume", ConsumerTool::example))),
            // Tool installation support
            ToolFinder.ofInstaller(ToolInstaller.Mode.INSTALL_IMMEDIATE)
                // dedicated installer
                .with(new Ant("1.10.14"))
                .with(new GoogleJavaFormat("1.22.0"))
                .with(new Maven("3.9.6"))
                // convenient installer
                .withJavaApplication(
                    "rife2/bld@1.9.0",
                    "https://github.com/rife2/bld/releases/download/1.9.0/bld-1.9.0.jar")
                .withJavaApplication(
                    "kordamp/jarviz@0.3.0",
                    "https://github.com/kordamp/jarviz/releases/download/v0.3.0/jarviz-tool-provider-0.3.0.jar"));

    System.out.println(toString(finder));
  }

  record Noop(String name, int code) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      return code;
    }
  }

  record RunnableTool(String name, Runnable runnable) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      try {
        runnable.run();
        return 0;
      } catch (RuntimeException exception) {
        return 1;
      }
    }
  }

  record CallableTool(String name, Callable<Integer> callable) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      try {
        return callable.call();
      } catch (Exception exception) {
        return 1;
      }
    }
  }

  record ConsumerTool(String name, Consumer<String[]> consumer) implements ToolProvider {
    static void example(String... args) {}

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      try {
        consumer.accept(args);
        return 0;
      } catch (RuntimeException exception) {
        return 1;
      }
    }
  }

  static String toString(ToolFinder finder) {
    var joiner = new StringJoiner("\n");
    var width = "jar".length();
    var tools = finder.tools();
    var names = new TreeMap<String, List<Tool>>();
    for (var tool : tools) {
      var name = tool.identifier().name();
      names.computeIfAbsent(name, _ -> new ArrayList<>()).add(tool);
      var length = name.length();
      if (length > width) width = length;
    }
    for (var entry : names.entrySet()) {
      var all =
          entry.getValue().stream()
              .map(tool -> tool.identifier().toNamespaceAndNameAndVersion())
              .toList();
      var line = "%" + width + "s %s %s";
      joiner.add(line.formatted(entry.getKey(), "->", all.getFirst()));
      for (var next : all.stream().skip(1).toList()) {
        joiner.add(line.formatted("", "  ", next));
      }
    }
    var size = tools.size();
    joiner.add("    %d tool%s".formatted(size, size == 1 ? "" : "s"));
    return joiner.toString().stripTrailing();
  }
}
