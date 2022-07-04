package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ExternalPropertiesStorage;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ArgVester;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Optional;
import java.util.StringJoiner;

public class Import implements ToolOperator {
  public record CLI(String library, Optional<String> from) {}

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var cli = ArgVester.create(CLI.class).parse(args);
    var library = cli.library();

    if (library.isEmpty() || library.equals("?")) {
      var storage = ExternalPropertiesStorage.find(cli.from().orElse(""));
      if (bach.configuration().isVerbose()) out.println("Using storage: " + storage);
      var list = storage.map().get("external-modules");
      if (list == null) return 0;
      list.forEach(file -> out.println(computeCommand(cli, file)));
      return 0;
    }

    var target = bach.configuration().paths().externalModules(cli.library() + ".properties");

    if (Files.notExists(target)) {
      bach.run("load", target, computeUri(cli));
    }

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
    if (from.startsWith("http:") || from.startsWith("https:")) return from;
    var storage =
        from.isEmpty() ? ExternalPropertiesStorage.DEFAULT : ExternalPropertiesStorage.find(from);
    return storage.uri("external-modules", cli.library());
  }
}
