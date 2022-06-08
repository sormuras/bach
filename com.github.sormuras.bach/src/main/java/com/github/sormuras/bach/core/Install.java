package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Install implements ToolOperator {
  @Override
  public String name() {
    return "install";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    for (var arg : args) {
      var path =
          arg.endsWith(".properties")
              ? Path.of(arg)
              : bach.configuration().paths().externalTools(arg + ".properties");
      generateLoadAndVerifyCalls(path).forEach(bach::run);
    }
    return 0;
  }

  List<ToolCall> generateLoadAndVerifyCalls(Path path) {
    var name = path.getFileName().toString();
    var tool = path.resolveSibling(name.substring(0, name.length() - 11));
    var properties = new Properties();
    try {
      properties.load(new StringReader(Files.readString(path)));
    } catch (Exception exception) {
      throw new RuntimeException("Reading entries failed: " + path);
    }
    var calls = new ArrayList<ToolCall>();
    for (var key : properties.stringPropertyNames()) {
      var source = properties.getProperty(key);
      var target = tool.resolve(key);
      calls.add(ToolCall.of("load-and-verify", target, source));
    }
    return List.copyOf(calls);
  }
}
