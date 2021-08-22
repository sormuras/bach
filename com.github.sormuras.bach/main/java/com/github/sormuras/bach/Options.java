package com.github.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/** Options. */
public record Options(
    MainOptions forMain,
    ConfigurationOptions forConfiguration,
    ProjectOptions forProject,
    List<String> unhandledArguments) {

  /** A group of optional components used by the main program. */
  public record MainOptions(Optional<Boolean> help, Optional<Boolean> version) {}

  /** A group of optional components used as global settings of a {@link Configuration} instance. */
  public record ConfigurationOptions(
      Optional<Boolean> verbose, Optional<Boolean> lenient, Optional<Integer> timeout) {}

  /** A group of optional components used when building a project model instance. */
  public record ProjectOptions(Optional<String> name, Optional<Version> version) {}

  /** Parses an array of strings in command-line style into an options instance. */
  public static Options parse(String... args) {
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
