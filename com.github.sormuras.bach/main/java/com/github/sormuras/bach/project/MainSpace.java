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
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record MainSpace(
    List<String> modules,
    List<String> moduleSourcePaths,
    List<String> modulePaths,
    int release,
    boolean generateApiDocumentation,
    Map<String, List<String>> tweaks)
    implements Space {

  @Override
  public String name() {
    return "";
  }

  @Override
  public Path classes(Project project) {
    return project.base().classes("", release);
  }

  /**
   * @param project the project providing a workspace directory
   * @param entry the path (directory or regular file) to resolve
   * @return a path within the given project's documentation workspace
   */
  public Path documentation(Project project, String entry) {
    return project.base().workspace("documentation", entry);
  }
}
