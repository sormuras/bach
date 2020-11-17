package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Project;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A space for main modules.
 *
 * @param modules the list of modules to compile
 * @param moduleSourcePaths the list of module source path patterns
 * @param modulePaths the list of module paths
 * @param release the Java version (release feature number) to compile for
 * @param jarslug the {@code MODULE-NAME "@" jarslug ".jar"} part of the JAR file name
 * @param generateApiDocumentation {@code true} to enable a {@code javadoc} run
 * @param generateCustomRuntimeImage {@code true} to enable a {@code jlink} run
 * @param generateApplicationPackage {@code true} to enable a {@code jpackage} run
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record MainSpace(
    List<String> modules,
    List<String> moduleSourcePaths,
    List<String> modulePaths,
    int release,
    String jarslug,
    boolean generateApiDocumentation,
    boolean generateCustomRuntimeImage,
    boolean generateApplicationPackage,
    Map<String, List<String>> tweaks)
    implements Space {

  @Override
  public String name() {
    return "main";
  }

  /** @return a path to the classes directory of the main space */
  public Path classes() {
    return workspace("classes-" + name(), String.valueOf(release));
  }

  /**
   * @param entry the path (directory or regular file) to resolve
   * @return a path within the given project's documentation workspace
   */
  public Path documentation(String entry) {
    return Project.WORKSPACE.resolve("documentation/" + entry);
  }
}
