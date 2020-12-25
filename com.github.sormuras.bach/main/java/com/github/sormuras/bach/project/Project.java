package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor.Version;

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
 * @param externals the external modules manager
 * @param spaces the codes space component
 */
public record Project(String name, Version version, ExternalModules externals, CodeSpaces spaces) {

  /**
   * Returns a project model based on walking the current working directory.
   *
   * @return a project model
   */
  public static Project of() {
    return of(ProjectInfo.class.getModule().getAnnotation(ProjectInfo.class));
  }

  /**
   * Returns a project model based on the current working directory and the given info annotation.
   *
   * @param info the project info annotation
   * @return a project model
   */
  public static Project of(ProjectInfo info) {
    return new ProjectBuilder(info).build();
  }
}
