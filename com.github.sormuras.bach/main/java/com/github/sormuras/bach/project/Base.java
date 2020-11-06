package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A collection of project-defining directories and derived paths.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * Component     Directory Tree Example
 * --------------------------------------------------------
 * directory --> jigsaw-quick-start
 *               ├───.bach
 *               │   ├───build
 *               │   │       module-info.java
 *               │   ├───cache
 *               │   │       com.github.sormuras.bach@16.jar
 *               │   │       (more tools and plugins...)
 * libraries --> │   ├───libraries
 *               │   │       org.junit.jupiter.jar
 *               │   │       (more external modules...)
 * workspace --> │   └───workspace
 *               │       ├───classes
 *               │       │   └───11
 *               │       │       ├───com.greetings
 *               │       │       │       module-info.class
 *               │       │       └───com
 *               │       │           └───greetings
 *               │       │                   Main.class
 *               │       ├───classes-test
 *               │       ├───documentation
 *               │       ├───image
 *               │       ├───modules
 *               │       │       com.greetings@2020.jar
 *               │       ├───modules-test
 *               │       ├───reports
 *               │       └───sources
 *               └───com.greetings
 *                   │   module-info.java
 *                   └───com
 *                       └───greetings
 *                               Main.java
 * }</pre>
 *
 * @param directory the path to the base directory of the project
 * @param libraries the directory that contains 3rd-party modules
 * @param workspace the directory that collects all generated assets
 */
public record Base(Path directory, Path libraries, Path workspace) {

  private static final Path EMPTY_PATH = Path.of("");
  private static final Path DEFAULT_LIBRARIES = Path.of(".bach/libraries");
  private static final Path DEFAULT_WORKSPACE = Path.of(".bach/workspace");

  /**
   * Default (empty) base instance point to the current user directory.
   *
   * @see #ofCurrentDirectory()
   */
  public static final Base DEFAULT = new Base(EMPTY_PATH, DEFAULT_LIBRARIES, DEFAULT_WORKSPACE);

  /**
   * Returns a base instance for the current user directory.
   *
   * <ul>
   *   <li>{@code directory} = {@code Path.of("")}
   *   <li>{@code libraries} = {@code Path.of(".bach", "libraries")}
   *   <li>{@code workspace} = {@code Path.of(".bach", "workspace")}
   * </ul>
   *
   * @return the default base object
   * @see #DEFAULT
   */
  public static Base ofCurrentDirectory() {
    return DEFAULT;
  }

  /**
   * Returns a base instance for the given directory.
   *
   * <ul>
   *   <li>{@code directory} = {@code Path.of(first, more)}
   *   <li>{@code libraries} = {@code Path.of(first, more, ".bach", "libraries")}
   *   <li>{@code workspace} = {@code Path.of(first, more, ".bach", "workspace")}
   * </ul>
   *
   * @param first The first path element to use as the base directory
   * @param more The array of path elements to complete the base directory
   * @return a base object initialized for the given directory
   */
  public static Base of(String first, String... more) {
    return of(Path.of(first, more));
  }

  /**
   * Returns a base instance for the given directory.
   *
   * <ul>
   *   <li>{@code directory} = {@code directory}
   *   <li>{@code libraries} = {@code directory.resolve(".bach", "libraries")}
   *   <li>{@code workspace} = {@code directory.resolve(".bach", "workspace")}
   * </ul>
   *
   * @param directory The path to use as the base directory
   * @return a base object initialized for the given directory
   */
  public static Base of(Path directory) {
    var base = directory.normalize();
    if (base.toString().equals("")) return DEFAULT;
    return new Base(base, base.resolve(DEFAULT_LIBRARIES), base.resolve(DEFAULT_WORKSPACE));
  }

  /** @return {@code true} if this is or equals the default base instance */
  public boolean isDefault() {
    return this == DEFAULT || directory.equals(EMPTY_PATH) && isDefaultIgnoreBaseDirectory();
  }

  /** @return {@code true} if this is or equals the default base instance */
  public boolean isDefaultIgnoreBaseDirectory() {
    return libraries.equals(directory.resolve(DEFAULT_LIBRARIES))
        && workspace.equals(directory.resolve(DEFAULT_WORKSPACE));
  }

  /**
   * Returns a resolved path.
   *
   * @param entry first path to resolve
   * @param more more paths to resolve
   * @return a resolved path
   */
  public Path directory(String entry, String... more) {
    return directory.resolve(Path.of(entry, more));
  }

  /**
   * Returns a resolved path.
   *
   * @param entry first path to resolve
   * @param more more paths to resolve
   * @return a resolved path
   */
  public Path workspace(String entry, String... more) {
    return workspace.resolve(Path.of(entry, more));
  }

  /**
   * Returns a resolved path.
   *
   * @param space the code space to resolve
   * @param release the Java release feature number to resolve
   * @return a resolved path
   */
  public Path classes(String space, int release) {
    return workspace("classes" + suffix(space), String.valueOf(release));
  }

  /**
   * Returns a resolved path.
   *
   * @param space the code space to resolve
   * @param release the Java release feature number to resolve
   * @param module the name of the module to resolve
   * @return a resolved path
   */
  public Path classes(String space, int release, String module) {
    return classes(space, release).resolve(module);
  }

  private static String suffix(String name) {
    return name.isEmpty() || "main".equals(name) ? "" : "-" + name;
  }

  /**
   * @return the file name of the base directory as a string
   * @throws java.util.NoSuchElementException if no file name is present
   */
  public String toName() {
    return Optional.ofNullable(directory.getFileName()).map(Path::toString).orElseThrow();
  }
}
