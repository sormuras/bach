package com.github.sormuras.bach.project;

import java.util.Optional;

/**
 * A launcher command configuration.
 *
 * @param name the (file) name of the launcher
 * @param module the module to launch
 */
public record Launcher(String name, String module) {

  /** @return an optional launch command consumable by {@code jlink --launcher} */
  public Optional<String> command() {
    if (name.isEmpty() || module.isEmpty()) return Optional.empty();
    return Optional.of(name + "=" + module);
  }
}
