package com.github.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public record Options(ProjectOptions project, SettingsOptions settings, List<String> unhandled) {

  public record SettingsOptions(Optional<Boolean> verbose, Optional<Integer> timeout) {}

  public record ProjectOptions(Optional<String> name, Optional<Version> version) {}

  public static Options of(String... args) {
    Boolean verbose = null;
    Integer timeout = null;
    String projectName = null;
    Version projectVersion = null;
    var unhandled = new LinkedList<String>();
    var arguments = new LinkedList<>(List.of(args));
    while (!arguments.isEmpty()) {
      var argument = arguments.pop();
      switch (argument) {
        case "--verbose" -> verbose = true;
        case "--timeout" -> timeout = Integer.parseInt(arguments.pop());
        case "--project-name" -> projectName = arguments.pop();
        case "--project-version" -> projectVersion = Version.parse(arguments.pop());
        default -> unhandled.add(argument);
      }
    }
    return new Options(
        new ProjectOptions(Optional.ofNullable(projectName), Optional.ofNullable(projectVersion)),
        new SettingsOptions(Optional.ofNullable(verbose), Optional.ofNullable(timeout)),
        List.copyOf(unhandled));
  }
}
