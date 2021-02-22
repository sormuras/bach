package com.github.sormuras.bach.project;

import java.nio.file.Path;

/**
 * A record of well-known directories.
 *
 * @param root the root directory of the project
 * @param bin the directory that contains Bach's binary files
 * @param externalModules the directory to store and find all external modules
 * @param externalTools the directory to store and find external tools
 * @param workspace the directory to store compiled files to
 */
public record Folders(
    Path root, Path bin, Path externalModules, Path externalTools, Path workspace) {

  private static final Path BIN = Path.of(".bach/bin");
  private static final Path EXTERNAL_MODULES = Path.of(".bach/external-modules");
  private static final Path EXTERNAL_TOOLS = Path.of(".bach/external-tools");
  private static final Path WORKSPACE = Path.of(".bach/workspace");

  /**
   * {@return an instance with default values resolved to the given root directory}
   *
   * @param root the root directory of the project
   */
  public static Folders of(String root) {
    return of(Path.of(root));
  }

  /**
   * {@return an instance with default values resolved to the given root directory}
   *
   * @param root the root directory of the project
   */
  public static Folders of(Path root) {
    var bin = root.resolve(BIN);
    var externalModules = root.resolve(EXTERNAL_MODULES);
    var externalTools = root.resolve(EXTERNAL_TOOLS);
    var workspace = root.resolve(WORKSPACE);
    return new Folders(root, bin, externalModules, externalTools, workspace);
  }

  /** {@return a file or folder below the root directory of this project} */
  public Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  /** {@return a file or folder below the directory for external modules} */
  public Path externalModules(String first, String... more) {
    return externalModules.resolve(Path.of(first, more));
  }

  /** {@return a file or folder below the directory for external tools} */
  public Path externalTools(String first, String... more) {
    return externalTools.resolve(Path.of(first, more));
  }

  /** {@return a file or folder below the workspace directory} */
  public Path workspace(String first, String... more) {
    return workspace.resolve(Path.of(first, more));
  }
}
