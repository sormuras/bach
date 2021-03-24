package com.github.sormuras.bach.project;

/**
 * A launcher command configuration used by {@code jlink}.
 *
 * <p>{@code --launcher command=module or --launcher command=module/main}
 *
 * <p>Specifies the launcher command name for the module or the command name for the module and main
 * class (the module and the main class names are separated by a slash ({@code /})).
 *
 * @param command the (file) name of the launcher
 * @param module the module to launch
 * @param main the possibly empty name of the main class
 */
public record ModuleLauncher(String command, String module, String main) {
  public String value() {
    return command + '=' + module + (main.isEmpty() ? "" : '/' + main);
  }
}
