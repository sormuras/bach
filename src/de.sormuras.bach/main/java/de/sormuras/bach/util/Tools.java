package de.sormuras.bach.util;

import de.sormuras.bach.Call;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** Tool registry. */
public class Tools {

  final Map<String, ToolProvider> map;

  public Tools() {
    this.map = new TreeMap<>();
    ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).stream()
        .map(ServiceLoader.Provider::get)
        .forEach(provider -> map.putIfAbsent(provider.name(), provider));
  }

  public ToolProvider get(String name) {
    var tool = map.get(name);
    if (tool == null) {
      throw new NoSuchElementException("No such tool: " + name);
    }
    return tool;
  }

  public void forEach(Consumer<ToolProvider> action) {
    map.values().forEach(action);
  }

  public int launch(String name, Collection<String> arguments) {
    return launch(name, arguments, true);
  }

  public int launch(String name, Collection<String> arguments, boolean inheritIO) {
    var tool = map.get(name);
    if (tool != null) return tool.run(System.out, System.err, arguments.toArray(String[]::new));

    var command = new Call("Call of tool: " + name, true);
    if (name.equals("mvn")) {
      command.iff(
          System.getProperty("os.name").toLowerCase().contains("win"),
          c -> c.add("cmd", "/C"),
          c -> c.add("/usr/bin/env", "--"));
    }
    command.add(name);
    command.forEach(arguments, Call::add);
    try {
      var builder = new ProcessBuilder(command.toList(false));
      if (inheritIO) builder.inheritIO();
      var process = builder.start();
      if (!inheritIO) {
        process.getInputStream().transferTo(System.out);
        process.getErrorStream().transferTo(System.err);
      }
      return process.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
      return 1;
    }
  }
}
