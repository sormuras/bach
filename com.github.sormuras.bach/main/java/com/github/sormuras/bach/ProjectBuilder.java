package com.github.sormuras.bach;

/** Builds a modular Java project. */
public class ProjectBuilder {

  private final Bach bach;
  private final Project project;

  /**
   * Initialize this project builder with the given components.
   *
   * @param bach the {@code Bach} instance
   * @param project the project to build
   */
  public ProjectBuilder(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
  }

  /** Builds a modular Java project. */
  public void build() {
    bach.printStream().println("Work on " + project);
    // main: javac + jar
    // main: javadoc
    // main: jlink
    // test: javac + jar
    // test: junit
    // test-preview: javac + jar
    // test-preview: junit
  }
}
