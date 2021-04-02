package com.github.sormuras.bach.project;

import java.util.Optional;

/**
 * A code space for main modules.
 *
 * @param declarations the list of main modules to compile
 * @param modulePaths the list of module paths
 * @param release the Java version (release feature number) to compile for
 * @param launcher an optional launcher component used by {@code jlink}
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record MainSpace(
    LocalModules declarations,
    ModulePaths modulePaths,
    int release,
    Optional<ModuleLauncher> launcher,
    Tweaks tweaks)
    implements Space {
  @Override
  public String name() {
    return "main";
  }
}
