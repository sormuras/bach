package com.github.sormuras.bach.project;

import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.internal.Paths;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

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
 *               │       ├───classes-test-preview
 *               │       ├───documentation
 *               │       ├───image
 *               │       ├───modules
 *               │       │       com.greetings@2020.jar
 *               │       ├───modules-test
 *               │       ├───modules-test-preview
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
 * @param spaces the codes space component
 */
public record Project(String name, Version version, Library library, CodeSpaces spaces) {

  /**
   * Returns a project model based on walking the current working directory.
   *
   * @return a project model
   */
  public static Project of() {
    var name = Paths.name(Path.of(""), "unnamed");
    var info = ProjectInfo.class.getModule().getAnnotation(ProjectInfo.class);
    return new ProjectBuilder(name, info).build();
  }

  /**
   * Returns a project model based on the current working directory and the given info annotation.
   *
   * @param info the project info annotation
   * @return a project model
   */
  public static Project of(ProjectInfo info) {
    return new ProjectBuilder(info.name(), info).build();
  }
}
