package com.github.sormuras.bach;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** A tool reference. */
public record Tool(Set<Flag> flags, String name, ToolProvider provider) {

  public enum Flag {
    HIDDEN
  }

  public static Tool of(ToolProvider provider, Flag... flags) {
    var module = provider.getClass().getModule();
    var name = module.isNamed() ? module.getName() + '/' + provider.name() : provider.name();
    return new Tool(Set.of(flags), name, provider);
  }

  public static Tool ofNativeToolInJavaHome(String name) {
    var executable = Path.of(System.getProperty("java.home"), "bin", name);
    return ofNativeTool("java-home/" + name, List.of(executable.toString()));
  }

  public static Tool ofNativeTool(String name, List<String> command) {
    return Tool.of(new NativeToolProvider(name, command));
  }

  public Tool with(Flag flag) {
    var flags = Stream.concat(this.flags.stream(), Stream.of(flag)).toList();
    return new Tool(Set.copyOf(flags), name, provider);
  }

  public boolean isNotHidden() {
    return !flags.contains(Flag.HIDDEN);
  }

  public boolean isNameMatching(String text) {
    // name = "foo/bar" matches text = "foo/bar"
    // name = "foo/bar" matches text = "bar" because name ends with "/bar"
    return name.equals(text) || name.endsWith('/' + text);
  }

  record NativeToolProvider(String name, List<String> command) implements ToolProvider {
    record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
      @Override
      public void run() {
        new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
      }
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... arguments) {
      var builder = new ProcessBuilder(new ArrayList<>(command));
      builder.command().addAll(List.of(arguments));
      try {
        var process = builder.start();
        new Thread(new LinePrinter(process.getInputStream(), out)).start();
        new Thread(new LinePrinter(process.getErrorStream(), err)).start();
        return process.waitFor();
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return -1;
      }
    }
  }
}
