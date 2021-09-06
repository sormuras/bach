package com.github.sormuras.bach.command;

import java.util.List;

/** An option collecting names of Java modules to operate on. */
public record ModulesOption(List<String> values) implements Option.Values<String> {
  public static ModulesOption empty() {
    return new ModulesOption(List.of());
  }
}
