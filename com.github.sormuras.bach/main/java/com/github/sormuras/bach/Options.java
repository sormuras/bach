package com.github.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public record Options(
    MainOptions forMain,
    ConfigurationOptions forConfiguration,
    ProjectOptions forProject,
    List<String> unhandledArguments) {

  public record MainOptions(Optional<Boolean> help, Optional<Boolean> version) {}

  public record ConfigurationOptions(
      Optional<Boolean> verbose, Optional<Boolean> lenient, Optional<Integer> timeout) {}

  public record ProjectOptions(Optional<String> name, Optional<Version> version) {}

  public static Options of(String... args) {
    Boolean help = null;
    Boolean version = null;
    Boolean verbose = null;
    Boolean lenient = null;
    Integer timeout = null;
    String projectName = null;
    Version projectVersion = null;
    var unhandled = new LinkedList<String>();
    var arguments = new LinkedList<>(List.of(args));
    while (!arguments.isEmpty()) {
      var argument = arguments.pop();
      switch (argument) {
        case "--help", "/?" -> help = true;
        case "--version" -> version = true;
        case "--verbose" -> verbose = true;
        case "--lenient" -> lenient = true;
        case "--timeout" -> timeout = Integer.parseInt(arguments.pop());
        case "--project-name" -> projectName = arguments.pop();
        case "--project-version" -> projectVersion = Version.parse(arguments.pop());
        default -> unhandled.add(argument);
      }
    }
    return new Options(
        new MainOptions(Optional.ofNullable(help), Optional.ofNullable(version)),
        new ConfigurationOptions(
            Optional.ofNullable(verbose),
            Optional.ofNullable(lenient),
            Optional.ofNullable(timeout)),
        new ProjectOptions(Optional.ofNullable(projectName), Optional.ofNullable(projectVersion)),
        List.copyOf(unhandled));
  }
}
