package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.project.Library;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.TestSpace;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Bach's project model.
 *
 * <h2>Directory Tree Example</h2>
 *
 * <pre>{@code
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
 * @param name the name of the project
 * @param version the version of the project
 * @param library the external modules manager
 * @param main the main module space
 * @param test the test module space
 */
public record Project(String name, Version version, Library library, MainSpace main, TestSpace test) {

  /** @return a stream of all module names */
  public Stream<String> findAllModuleNames() {
    return Stream.concat(main.modules().stream(), test.modules().stream());
  }

  /** Path to directory with external modules. */
  public static final Path LIBRARIES = Path.of(".bach/libraries");

  /** Path to directory that collects all generated assets. */
  public static final Path WORKSPACE = Path.of(".bach/workspace");

  /**
   * Returns a project model based on walking the current workding directory.
   *
   * @return a project model
   */
  public static Project of() {
    var name = Paths.name(Path.of(""), "unnamed");
    var info = Bach.class.getModule().getAnnotation(ProjectInfo.class);
    return new ProjectFactory(name, info).newProject();
  }

  /**
   * Returns a project model based on the current workding directory and the given info annotation.
   *
   * @param info the project info annotation
   * @return a project model
   */
  public static Project of(ProjectInfo info) {
    return new ProjectFactory(info.name(), info).newProject();
  }
}
