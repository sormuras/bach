package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ProjectInfo;
import java.util.stream.Stream;

/** A feature toggle option. */
public enum Flag {

  /**
   * Print messages about what Bach is doing.
   *
   * @see Bach#log(String, Object...)
   */
  VERBOSE("Print messages about what Bach is doing."),

  /**
   * Mute all normal (expected) printouts.
   *
   * @see Bach#say(String, Object...)
   */
  SILENT("Mute all normal (expected) printouts."),

  /** Print Bach's version and exit. */
  VERSION("Print Bach's version and exit."),

  /** Print Bach's version and continue. */
  SHOW_VERSION("Print Bach's version and continue."),

  /** Print usage information and exit. */
  HELP("Print usage information and exit."),

  /**
   * Activate all best-effort guessing magic.
   *
   * <ul>
   *   <li>Lookup external modules via `sormuras/modules`
   *   <li>Lookup external modules via GitHub releases
   * </ul>
   *
   * @see com.github.sormuras.bach.lookup.SormurasModulesModuleLookup
   * @see com.github.sormuras.bach.lookup.GitHubReleasesModuleLookup
   */
  GUESS("Activate all best-effort guessing magic."),

  /**
   * Activate all verification measures available.
   *
   * <p>When Bach runs in strict mode the following default implementations change their behaviour
   * into a more defensive way of doing things:
   *
   * <ul>
   *   <li>Source code style is checked via running a formatter in `VERIFY` mode.
   *   <li>Only explicitly declared requires are considered when downloading external modules.
   *   <li>All external modular JAR files are verified to conform with expected metadata.
   * </ul>
   *
   * @see ProjectInfo.Options#formatSourceFilesWithCodeStyle()
   * @see ProjectInfo.Libraries#metadata()
   */
  STRICT("Activate all verification measures available."),

  /**
   * Include all files found in source folders into their modular JAR files.
   *
   * @see ProjectInfo.Options#includeSourceFilesIntoModules()
   */
  JAR_WITH_SOURCES("Include all files found in source folders into their modular JAR files."),

  /**
   * Prevent parallel execution of commands.
   *
   * @see Bach#run(Command, Command[])
   * @see Bach#run(Stream)
   */
  RUN_COMMANDS_SEQUENTIALLY("Prevent parallel execution of commands.");

  public final String help;

  Flag(String help) {
    this.help = help;
  }
}
