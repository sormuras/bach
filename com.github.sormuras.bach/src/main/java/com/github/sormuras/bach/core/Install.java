package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ExternalPropertiesStorage;
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
import java.util.StringJoiner;

public class Install implements ToolOperator {
  public record CLI(String tool, Optional<String> from) {}

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var cli = ArgVester.create(CLI.class).parse(args);
    var tool = cli.tool();

    if (tool.isEmpty() || tool.equals("?")) {
      var storage = ExternalPropertiesStorage.find(cli.from().orElse(""));
      if (bach.configuration().isVerbose()) out.println("Using storage: " + storage);
      var list = storage.map().get("external-tools");
      if (list == null) return 0;
      list.forEach(file -> out.println(computeCommand(cli, file)));
      return 0;
    }

    var target = bach.configuration().paths().externalTools(tool + ".properties");

    if (Files.notExists(target)) {
      bach.run("load", target, computeUri(cli));
    }

    generateLoadAndVerifyCalls(target).forEach(bach::run);

    return 0;
  }

  String computeCommand(CLI cli, String file) {
    var command = new StringJoiner(" ");
    command.add("bach");
    command.add(name()); // "install"
    command.add(file);
    cli.from().ifPresent(storage -> command.add("--from").add(storage));
    return command.toString();
  }

  String computeUri(CLI cli) {
    var from = cli.from().orElse("");
    if (from.startsWith("http")) return from;
    var storage =
        from.isEmpty() ? ExternalPropertiesStorage.DEFAULT : ExternalPropertiesStorage.find(from);
    return storage.uri("external-tools", cli.tool());
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
      if (key.startsWith("@")) continue;
      var source = properties.getProperty(key);
      var target = tool.resolve(key);
      calls.add(ToolCall.of("load-and-verify", target, source));
    }
    return List.copyOf(calls);
  }
}
