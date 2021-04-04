package com.github.sormuras.bach.project;

import com.github.sormuras.bach.ProjectInfo;

/** Key-value pair option. */
public enum Property {
  /** Name of the module to load on startup passed via {@code --bach-info MODULE}. */
  BACH_INFO("Specify the module to load on startup, defaults to \"" + ProjectInfo.MODULE + "\""),

  /**
   * Project's name passed via {@code --project-name NAME}.
   *
   * @see ProjectInfo#name()
   */
  PROJECT_NAME("Specify the name of the project."),

  /**
   * Project's version passed via {@code --project-version VERSION}.
   *
   * @see ProjectInfo#version()
   */
  PROJECT_VERSION("Specify the version of the project."),

  /**
   * Compile main modules for specified Java release.
   *
   * @see ProjectInfo.Options#compileModulesForJavaRelease()
   */
  PROJECT_TARGETS_JAVA("Compile main modules for specified Java release."),

  /** Add specified tool to the universe of executable tools. */
  LIMIT_TOOLS("Add specified tools to the universe of executable tools.", true),

  /** Skip all executions for the specified tool, this option is repeatable. */
  SKIP_TOOLS("Skip all executions of the specified tool.", true);

  public final String help;
  public final boolean repeatable;

  Property(String help) {
    this(help, false);
  }

  Property(String help, boolean repeatable) {
    this.help = help;
    this.repeatable = repeatable;
  }
}
