package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.Paths;
import java.io.File;
import java.nio.file.Path;

/** A nominal space for modular Java source code. */
/*sealed*/ interface CodeSpace /*permits MainCodeSpace, TestCodeSpace, TestPreviewCodeSpace*/ {

  /**
   * Returns all module declarations of this code space.
   *
   * @return an object that holds all module declarations
   */
  ModuleDeclarations modules();

  /**
   * Returns a module paths object.
   *
   * @return a module paths object
   */
  ModulePaths modulePaths();

  /**
   * Returns the possibly empty name of this space.
   *
   * @return an empty string for the main space, else a non-empty name
   */
  String name();

  /**
   * Returns the additional arguments to be passed on a per-tool basis.
   *
   * @return the additional arguments to be passed on a per-tool basis
   */
  Tweaks tweaks();

  /**
   * Returns a normalized path resolved to the workspace directory.
   *
   * @param entry first path to resolve
   * @param more more paths to resolve
   * @return a resolved and normalized path
   */
  default Path workspace(String entry, String... more) {
    return Bach.WORKSPACE.resolve(Path.of(entry, more)).normalize();
  }

  /**
   * Returns a resolved path.
   *
   * @return a path to the classes directory of this code space
   */
  default Path classes() {
    return classes(Runtime.version().feature());
  }

  /**
   * Returns a resolved path.
   *
   * @param release the Java release feature number to resolve
   * @return a resolved path
   */
  default Path classes(int release) {
    return workspace("classes-" + name(), String.valueOf(release));
  }

  /**
   * Returns a resolved path.
   *
   * @param release the Java release feature number to resolve
   * @param module the name of the module to resolve
   * @return a resolved path
   */
  default Path classes(int release, String module) {
    return classes(release).resolve(module);
  }

  /** @return a string usable for {@code --module-source-path} */
  default String toModuleSourcePath() {
    return String.join(File.pathSeparator, modules().toModuleSourcePaths(false));
  }

  /** @return a string usable for {@code --module-path} */
  default String toModulePath() {
    return Paths.join(modulePaths().paths());
  }
}
