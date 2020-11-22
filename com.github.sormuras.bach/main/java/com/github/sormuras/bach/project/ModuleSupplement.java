package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;

/**
 * Additional module-related information.
 */
public record ModuleSupplement(Path info, ModuleDescriptor descriptor, List<Integer> releases) {

  /**
   *
   * @return {@code true} if there two or more release feature numbers listed in {@link #releases}
   */
  public boolean isMultiRelease() {
    return releases.size() >= 2;
  }
}
