package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ArgVester;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class Install implements ToolOperator {
  public record CLI(
      String tool, Optional<String> uri, Optional<String> user, Optional<String> repo) {}

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var cli = ArgVester.create(CLI.class).parse(args);
    var target = bach.configuration().paths().externalTools(cli.tool() + ".properties");

    if (Files.notExists(target)) {
      var uri = cli.uri().orElse(computeUri(cli));
      bach.run("load", target, uri);
    }

    generateLoadAndVerifyCalls(target).forEach(bach::run);

    return 0;
  }

  String computeUri(CLI cli) {
    return "https://github.com/"
        + cli.user().orElse("sormuras")
        + "/"
        + cli.repo().orElse("bach-external-tools")
        + "/raw/main/properties/"
        + cli.tool().split("@")[0]
        + "/"
        + cli.tool()
        + ".properties";
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
